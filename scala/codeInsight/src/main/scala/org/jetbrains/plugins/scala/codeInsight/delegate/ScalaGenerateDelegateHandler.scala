package org.jetbrains.plugins.scala.codeInsight.delegate

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.generation.DelegateMethodHandler
import consulo.language.editor.localize.CodeInsightLocalize
import consulo.application.ApplicationManager
import consulo.codeEditor.Editor
import consulo.document.FileDocumentManager
import consulo.project.Project
import com.intellij.java.language.psi.{CommonClassNames, PsiMethod}
import consulo.language.psi.{PsiDocumentManager, PsiElement, PsiFile}
import consulo.language.psi.scope.LocalSearchScope
import consulo.language.psi.search.ReferencesSearch
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.util.IncorrectOperationException
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements._
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.{ScParameterClause, ScTypeParam, ScTypeParamClause}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory._
import org.jetbrains.plugins.scala.lang.psi.types.{PhysicalMethodSignature, ScTypeExt}
import org.jetbrains.plugins.scala.lang.resolve.processor.CompletionProcessor
import org.jetbrains.plugins.scala.lang.resolve.{ResolveUtils, ScalaResolveResult}
import org.jetbrains.plugins.scala.overrideImplement

import scala.annotation.nowarn
import scala.collection.immutable.ArraySeq

@ExtensionImpl
final class ScalaGenerateDelegateHandler extends DelegateMethodHandler {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  import overrideImplement._

  override def isValidFor(editor: Editor, file: PsiFile): Boolean =
    targetElements(file, editor).nonEmpty

  override def startInWriteAction(): Boolean = false

  // upstream: modal member choosers, the Consulo member chooser is asynchronous
  override def invoke(@NotNull project: Project, @NotNull editor: Editor, @NotNull file: PsiFile): Unit = {
    if (!FileDocumentManager.getInstance.requestWriting(editor.getDocument, project))
      return
    PsiDocumentManager.getInstance(project).commitAllDocuments()

    chooseTarget(file, editor) { target =>
      chooseMethods(target, file, editor) { candidates =>
        if (candidates.nonEmpty)
          generateDelegates(target, candidates, file, editor)(using project)
      }(using project)
    }(using project)
  }

  private def generateDelegates(target: ClassMember, candidates: Seq[ScMethodMember], file: PsiFile, editor: Editor)
                               (implicit project: Project): Unit =
    inWriteCommandAction {
      try {
        val aClass = findClassAtCaret(file, editor)

        //Insert members with dummy body `???` and without override modifier
        val insertedMembers = ScalaGenerateMembersUtil.insertMembersAtCaretPosition(candidates, aClass, editor, addOverrideModifierIfEnforcedBySettings = false)

        //For every inserted method generate actual delegated method body and add "override" modifier if needded
        insertedMembers.foreach { insertedMember =>
          val newFunction = insertedMember.getPsiMember.asInstanceOf[ScFunctionDefinition]

          val body = methodBody(target, newFunction, target.getPsiElement)
          newFunction.body.foreach(_.replace(body))

          val needsOverrideModifier = newFunction.superMethod.nonEmpty
          if (needsOverrideModifier) {
            newFunction.setModifierProperty("override")
          }
        }

        insertedMembers.lastOption.foreach(_.positionCaret(editor, toEditMethodBody = true))
      }
      catch {
        case _: IncorrectOperationException =>
          throw new IncorrectOperationException(s"Could not delegate methods to ${target.getText}")
      }
    }

  private def methodBody(delegate: ClassMember, prototype: ScFunction, context: PsiElement): ScExpression = {
    def typeParameterUsedIn(parameter: ScTypeParam, elements: Seq[PsiElement]) = {
      elements.exists(elem =>
        !ReferencesSearch
          .search(parameter, new LocalSearchScope(elem))
          .findAll()
          .isEmpty
      )
    }

    val includeTypeArgsInCall = {
      val typeParams = prototype.typeParameters
      val parametersAndRetType = prototype.parameters ++ prototype.returnTypeElement
      typeParams.exists(!typeParameterUsedIn(_, parametersAndRetType))
    }

    val dText: String = delegateText(delegate)
    val methodName = prototype.name

    def typeClauseApplicationText(typeClause: ScTypeParamClause) =
      if (includeTypeArgsInCall)
        typeClause
          .typeParameters
          .map(_.nameId.getText)
          .commaSeparated(Model.SquareBrackets)
      else ""

    def paramClauseApplicationText(paramClause: ScParameterClause) =
      paramClause.parameters.map(_.name).commaSeparated(Model.Parentheses)

    val signatureArguments = prototype.signatureClauses.map {
      case ScSignatureClause.TypeClause(clause) => typeClauseApplicationText(clause)
      case ScSignatureClause.TermClause(clause) => paramClauseApplicationText(clause)
    }.mkString

    createExpressionFromText(s"$dText.$methodName$signatureArguments", context)(using prototype.getManager)
  }

  private def delegateText(delegate: ClassMember): String = {
    val delegateText = (delegate match {
      case field: ScalaFieldMember => field.name
      case ScMethodMember(PhysicalMethodSignature(method, _), _) =>
        method match {
          case m: PsiMethod if m.isAccessor => m.name
          case f: ScFunction if f.isEmptyParen => f.name + "()"
          case f: ScFunction if f.isParameterless => f.name
        }
    }): @nowarn("msg=exhaustive")
    delegateText
  }

  //TODO 1: Select all methods from delegate which do not yet exist like in Java
  // (see com.intellij.codeInsight.generation.GenerateDelegateHandler.chooseMethods)
  private def chooseMethods(delegate: ClassMember, file: PsiFile, editor: Editor)
                           (onChosen: Seq[ScMethodMember] => Unit)
                           (implicit project: Project): Unit = {
    val delegateType = delegate.asInstanceOf[ScalaTypedMember].scType
    val aClass = findClassAtCaret(file, editor)
    if (aClass == null) return
    val tBody = aClass.extendsBlock.templateBody.get
    val place = createExpressionWithContextFromText(delegateText(delegate), tBody, tBody.getFirstChild)

    val candidates = CompletionProcessor.variants(delegateType, place)
    val members = toMethodMembers(candidates, place)

    if (ApplicationManager.getApplication.isUnitTestMode)
      onChosen(members)
    else
      ScalaMemberChooser.show[ScMethodMember](members, false, true, false, true, false, aClass,
        CodeInsightLocalize.generateDelegateMethodChooserTitle().get)(onChosen)
  }

  private def toMethodMembers(candidates: Iterable[ScalaResolveResult], place: PsiElement): ArraySeq[ScMethodMember] = {
    def toMember(srr: ScalaResolveResult): Option[ScMethodMember] = {
      val ScalaResolveResult(element, subst) = srr: @unchecked

      if (srr.implicitFunction.nonEmpty) return None
      element match {
        case method: PsiMethod if method.isConstructor || method.containingClass == null => None
        case method: PsiMethod if method.containingClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT => None
        case method: PsiMethod if !ResolveUtils.isAccessible(method, place, forCompletion = true) => None
        case method: PsiMethod => Some(ScMethodMember(method, subst))
        case _ => None
      }
    }

    candidates.iterator.flatMap(toMember).to(ArraySeq)
  }

  private def chooseTarget(file: PsiFile, editor: Editor)
                          (onChosen: ClassMember => Unit)
                          (implicit project: Project): Unit = {
    val elements = targetElements(file, editor)
    if (elements.isEmpty) ()
    else if (ApplicationManager.getApplication.isUnitTestMode) onChosen(elements.head)
    else
      ScalaMemberChooser.show[ClassMember](elements, false, false, false, false, false, findClassAtCaret(file, editor),
        CodeInsightLocalize.generateDelegateTargetChooserTitle().get)(_.headOption.foreach(onChosen))
  }

  private def targetElements(file: PsiFile, editor: Editor): ArraySeq[ClassMember] = {
    val parents = findParentClassesAtCaret(file, editor)
    parents.iterator
      .flatMap(targetsIn)
      .to(ArraySeq)
      .sortBy(m => (m.getPsiElement.getContainingFile.name, m.getPsiElement.getTextRange.getStartOffset))
  }

  // todo add ScObjectMember for targets
  private def targetsIn(clazz: ScTemplateDefinition): Seq[ClassMember] =
    ScalaOIUtil.getAllMembersToOverride(clazz)
      .filter(canBeTargetInClass(_, clazz))

  private def canBeTargetInClass(member: ClassMember, clazz: ScTemplateDefinition): Boolean = member match {
    case _: ScAliasMember => false
    case typed: ScalaTypedMember if typed.scType.isUnit => false
    case ScMethodMember(PhysicalMethodSignature(method, _), _) =>
      method match {
        case m: PsiMethod if isMethodFromJavaLangObject(m) => false
        case f: ScFunction => (f.isParameterless || f.isEmptyParen) && ResolveUtils.isAccessible(f, clazz)
        case m: PsiMethod => m.isAccessor && ResolveUtils.isAccessible(m, clazz)
      }
    case fieldMember: ScalaFieldMember => ResolveUtils.isAccessible(fieldMember.getElement, clazz)
    case _ => false
  }

  private def isMethodFromJavaLangObject(m: PsiMethod): Boolean = {
    val cl = m.containingClass
    cl != null && cl.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT
  }

  private def findClassAtCaret(file: PsiFile, editor: Editor): ScTemplateDefinition = {
    val elementAtCaret = ScalaGenerateMembersUtil.getElementAtCaretAdjustedForIndentationBasedSyntax(file, editor)
    PsiTreeUtil.getContextOfType(elementAtCaret, classOf[ScTemplateDefinition])
  }

  private def findParentClassesAtCaret(file: PsiFile, editor: Editor): Seq[ScTemplateDefinition] = {
    val closestClass = findClassAtCaret(file, editor)
    if (closestClass == null)
      return Seq.empty

    closestClass.withParentsInFile.toSeq.collect { case td: ScTemplateDefinition => td }
  }
}
