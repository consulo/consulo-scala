package org.jetbrains.plugins.scala.structureView

import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.application.ui.UISettings
import consulo.codeEditor.Editor
import consulo.dataContext.DataContext
import consulo.language.editor.ui.navigationBar.StructureAwareNavBarModelExtension
import consulo.language.Language
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, PsiNamedElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScCaseClause
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScDo, ScExpression, ScFunctionExpr, ScIf, ScMatch, ScNewTemplateDefinition, ScWhile}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.{ScClassParameter, ScParameter}
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScExtension, ScFunction, ScTypeAlias, ScValue, ScValueOrVariable, ScValueOrVariableDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScTemplateDefinition, ScTypeDefinitionLike}
import org.jetbrains.plugins.scala.lang.psi.api.{ScalaFile, ScalaPsiElement}
import org.jetbrains.plugins.scala.projectView.FileKind
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.structureView.element.{AbstractItemPresentation, Element}

// upstream: order = "first" (not allowed on platform extension points)
// upstream: editor breadcrumbs come from ScalaBreadcrumbsInfoProvider; in Consulo the navigation bar plays that role,
// so the leaf, the parent chain and the texts below follow the breadcrumbs provider
@ExtensionImpl(order = "before JavaNavBarExtension, before ScratchNavBarExtension")
class ScalaNavBarModelExtension extends StructureAwareNavBarModelExtension {
  import ScalaNavBarModelExtension.*

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  @Nullable
  override def adjustElement(psiElement: PsiElement): PsiElement = psiElement match {
    // When a Scala file is only the type named after the file, optionally with its companion,
    // showing both `MyClass.scala` and `MyClass` would duplicate the same navigation level.
    // Replace the file element itself with the representative type, so the navbar starts
    // from `MyClass` instead of the physical file node.
    case file: ScalaFile => singleTopLevelFileRepresentative(file).getOrElse(file)
    case _ => psiElement
  }

  override protected def acceptParentFromModel(psiElement: PsiElement): Boolean = psiElement match {
    // After `adjustElement` substitutes the file with its representative type,
    // accepting the original file as a parent would reintroduce the duplicate `MyClass.scala > MyClass` path.
    // Report that the file parent is not acceptable exactly when such a representative
    // exists, so the model skips the file node and keeps only the type node.
    case file: ScalaFile => singleTopLevelFileRepresentative(file).isEmpty
    case _ => true
  }

  private def singleTopLevelFileRepresentative(file: ScalaFile): Option[ScTypeDefinitionLike] =
    // `FileKind` owns the file-shape check: it returns a representative only for files whose
    // contents are the matching top-level type definition and, at most, its companion.
    FileKind.getForFile(file).map(_.representative)

  @RequiredReadAction
  override def getLeafElement(dataContext: DataContext): PsiElement = {
    if (!UISettings.getInstance().getShowMembersInNavigationBar) return null

    val file = dataContext.getData(PsiFile.KEY)
    val editor = dataContext.getData(Editor.KEY)
    if (!file.isInstanceOf[ScalaFile] || editor == null) return null

    val offset = editor.getCaretModel.getOffset
    val atCaret = Option(file.findElementAt(offset)).orElse(if (offset > 0) Option(file.findElementAt(offset - 1)) else None)
    atCaret
      .filter(_.getLanguage == getLanguage)
      .flatMap(_.withParentsInFile.find(isShown))
      .orNull
  }

  @Nullable
  @RequiredReadAction
  override def getParent(psiElement: PsiElement): PsiElement = psiElement match {
    case _: ScalaFile =>
      null
    case element: ScalaPsiElement if element.getContainingFile.isInstanceOf[ScalaFile] =>
      element.parentsInFile.find(isShown).getOrElse {
        val file = element.getContainingFile.asInstanceOf[ScalaFile]
        if (acceptParentFromModel(file)) file else null
      }
    case _ =>
      super.getParent(psiElement)
  }

  @Nullable
  override def getPresentableText(item: Any): String = getPresentableText(item, false)

  @Nullable
  override def getPresentableText(item: Any, forPopup: Boolean): String = item match {
    case file: ScalaFile =>
      file.name
    case element: ScalaPsiElement =>
      describe(element)
        .orElse(Element.forPsiElement(element).collect { case e: AbstractItemPresentation => e.presentableText })
        .orNull
    case _ =>
      null
  }
}

object ScalaNavBarModelExtension {
  private val PAR_HOLDER = "(...)"

  private val MAX_TEXT_LENGTH = 150
  private val MAX_STRING_LENGTH = 25

  // upstream: ScalaBreadcrumbsInfoProvider.acceptElement
  private def isShown(element: PsiElement): Boolean = {
    lazy val settings = ScalaProjectSettings.getInstance(element.getProject)

    element match {
      case newDef: ScNewTemplateDefinition => newDef.isAnonymous && settings.isBreadcrumbsClassEnabled
      case _: ScTemplateDefinition => settings.isBreadcrumbsClassEnabled
      case _: ScFunctionExpr => settings.isBreadcrumbsLambdaEnabled
      case _: ScFunction => settings.isBreadcrumbsFunctionEnabled
      case _: ScCaseClause | _: ScMatch => settings.isBreadcrumbsMatchEnabled
      // upstream: shown only with the val/def setting; here vals, vars, type aliases and extensions that are
      // class or file members are always shown, like their structure view nodes listed in the navbar popups
      case member@(_: ScValueOrVariable | _: ScTypeAlias | _: ScExtension) =>
        settings.isBreadcrumbsValDefEnabled || !member.asInstanceOf[ScMember].isLocal
      case _: ScMember => settings.isBreadcrumbsValDefEnabled
      case _: ScIf | _: ScWhile | _: ScDo => settings.isBreadcrumbsIfDoWhileEnabled
      case _ => false
    }
  }

  // upstream: ScalaBreadcrumbsInfoProvider.getElementInfo
  private def describe(element: ScalaPsiElement): Option[String] = element match {
    case newDef: ScNewTemplateDefinition if newDef.isAnonymous => Some(describeNewTemplate(newDef))
    case _: ScNewTemplateDefinition => None
    case clazz: ScTemplateDefinition => Option(clazz.name)
    case funExpr: ScFunctionExpr => Some("λ" + signature(None, funExpr.parameters))
    case fun: ScFunction => Some(describeFunction(fun))
    case member: ScMember => describeMember(member)
    case caseClause: ScCaseClause => Some(s"case ${limitString(caseClause.pattern.map(_.getText).getOrElse(""))} =>")
    case expr: ScExpression => describeExpression(expr)
    case named: ScNamedElement => Option(named.name)
    case _ => None
  }

  private def limitString(s: String, stub: String = PAR_HOLDER): String =
    if (s == null) "" else if (s.length < MAX_STRING_LENGTH) s else stub

  private def limitText(txt: String, stub: String = "...", limit: Int = MAX_TEXT_LENGTH): String =
    if (txt == null) "" else if (txt.length < limit) txt else txt.substring(0, limit - 1 - stub.length) + stub

  private def signature(name: Option[String], parameters: Iterable[ScParameter]): String =
    name.getOrElse("") +
      limitString(parameters.map(p => p.name + ": " + p.typeElement.map(_.getText).getOrElse("Any")).mkString("(", ", ", ")"))

  private def describeFunction(fun: ScFunction): String =
    if (fun.isConstructor) "this" + limitString(signature(None, fun.parameters))
    else signature(Option(fun.name), fun.parameters)

  private def describeNewTemplate(newDef: ScNewTemplateDefinition): String = {
    val constructor = newDef.firstConstructorInvocation match {
      case None => "Object"
      case Some(c) if c.arguments.nonEmpty => s"${c.typeElement.getText}$PAR_HOLDER"
      case Some(c) => c.typeElement.getText
    }
    val hasOtherTypes = newDef.extendsBlock.templateParents.exists(_.parentClauses.size > 1)
    val otherTypesHolder = if (hasOtherTypes) " with ..." else ""
    limitText(s"new $constructor$otherTypesHolder", limit = MAX_STRING_LENGTH * 2)
  }

  private def describeMember(member: ScMember): Option[String] = member match {
    case v: ScValueOrVariable =>
      val keyword = if (v.isInstanceOf[ScValue]) "val " else "var "
      // upstream: `var` members have no text and several bindings miss the closing parenthesis
      val names = v match {
        case definition: ScValueOrVariableDefinition =>
          definition.bindings match {
            case Seq(single) if definition.isSimple => single.name
            case _ => definition.pList.getText
          }
        case _ =>
          v.declaredElements.map(_.name).mkString(", ")
      }
      Some(keyword + limitString(names))
    case ext: ScExtension =>
      Some("extension" + ext.targetParameter.map(p => " " + signature(None, Seq(p))).getOrElse(""))
    case _: ScPrimaryConstructor =>
      None
    case parameter: ScClassParameter =>
      Option(parameter.name)
    case other =>
      Option(other.getName)
  }

  private def describeExpression(expr: ScExpression): Option[String] = expr match {
    case ifSt: ScIf => Some(s"if (${limitString(ifSt.condition.map(_.getText).getOrElse(""))}) {...}")
    case whileSt: ScWhile => Some(s"while(${limitString(whileSt.condition.map(_.getText).getOrElse(""), "...")})")
    case doWhileSt: ScDo => Some(s"do ... while(${limitString(doWhileSt.condition.map(_.getText).getOrElse(""), "...")})")
    case matchSt: ScMatch => Some(limitString(matchSt.expression.map(_.getText).getOrElse(PAR_HOLDER)) + " match {...}")
    case _ => None
  }
}
