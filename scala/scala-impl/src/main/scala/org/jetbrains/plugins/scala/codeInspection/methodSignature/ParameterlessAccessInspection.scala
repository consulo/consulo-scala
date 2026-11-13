package org.jetbrains.plugins.scala.codeInspection.methodSignature

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.language.psi.{PsiElement, PsiElementVisitor, PsiFile}
import com.intellij.java.language.psi.PsiMethod
import org.jetbrains.plugins.scala.annotator.quickfix.AddCallParenthesesQuickFix
import org.jetbrains.plugins.scala.codeInspection.PsiElementVisitorSimple
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.types._
import org.jetbrains.plugins.scala.project.ScalaFeatures.forPsiOrDefault
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker

import scala.annotation.tailrec

sealed abstract class ParameterlessAccessInspection extends LocalInspectionTool {

  import ParameterlessAccessInspection._

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case element if !isValid(element) =>
    case reference@ScReferenceExpression(method: PsiMethod) if isValid(method) =>
      val maybeTargetExpression = reference.getParent match {
        case parent if !isFixable(parent) => None
        case call: ScGenericCall if !findCall(call) => Some(call)
        case _: ScGenericCall => None
        case _ => Some(reference)
      }

      maybeTargetExpression.flatMap(collect(_, reference)).foreach { expr =>
        holder.registerProblem(reference.nameId, getDisplayName.get, new AddCallParenthesesQuickFix(expr))
      }
    case _ =>
  }

  protected def isValid(element: PsiElement): Boolean = element.isValid

  protected def isValid(method: PsiMethod): Boolean

  protected def isFixable(parent: PsiElement): Boolean = parent match {
    case _: ScMethodCall |
         _: ScInfixExpr |
         _: ScUnderscoreSection => false
    case _ => true
  }

  protected def collect(expression: ScExpression,
                        reference: ScReferenceExpression): Option[ScExpression]
}

object ParameterlessAccessInspection {

  @tailrec
  private[methodSignature] def findCall(element: PsiElement): Boolean = element.getContext match {
    case _: ScMethodCall => true
    case expression: ScParenthesisedExpr => findCall(expression)
    case call: ScGenericCall => findCall(call)
    case _ => false
  }

  private[methodSignature] object HasFunctionType {

    def unapply(expression: ScExpression): Option[Seq[ScType]] = {
      implicit val context: Context = Context(expression)

      expression match {
        case result.Typeable(api.FunctionType(_, seq)) => Some(seq)
        case _ => None
      }
    }
  }
}

@ExtensionImpl
final class ParameterlessAccessInspectionJavaMutator extends ParameterlessAccessInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.parameterless.access.java.mutator"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "JavaMutatorMethodAccessedAsParameterless"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import ParameterlessAccessInspection._

  override protected def collect(expression: ScExpression,
                                 reference: ScReferenceExpression): Option[ScExpression] = reference match {
    case HasFunctionType(_) => None
    case _ => Some(expression)
  }

  override protected def isValid(method: PsiMethod): Boolean = quickfix.isMutator(method)
}

@ExtensionImpl
final class ParameterlessAccessInspectionEmptyParenMethod extends ParameterlessAccessInspection {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("method.signature.parameterless.access.empty.paren"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getShortName: String = "EmptyParenMethodAccessedAsParameterless"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import ParameterlessAccessInspection._

  override def isAvailableForFile(file: PsiFile): Boolean =
    !file.isScala3 && super.isAvailableForFile(file)

  // might have been eta-expanded to () => A, so don't warn.
  // this avoids false positives. To be more accurate, we would need an 'etaExpanded'
  // flag in ScalaResolveResult.
  override protected def collect(expression: ScExpression,
                                 reference: ScReferenceExpression): Option[ScExpression] = expression match {
    case HasFunctionType(Seq()) => None
    case _ => Some(reference)
  }

  override protected def isValid(element: PsiElement): Boolean =
    super.isValid(element) && IntentionAvailabilityChecker.checkInspection(this, element)

  override protected def isValid(method: PsiMethod): Boolean = method match {
    case function: ScFunction =>
      function.isValid && !function.isInCompiledFile && function.isEmptyParen
    case _ => false
  }

  override protected def isFixable(parent: PsiElement): Boolean = parent match {
    case _: ScPrefixExpr => false
    case _ => super.isFixable(parent)
  }
}
