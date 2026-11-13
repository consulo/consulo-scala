package org.jetbrains.plugins.scala.codeInspection.parentheses

import consulo.language.editor.inspection.{LocalInspectionTool, LocalInspectionToolSession, LocalQuickFix, ProblemsHolder}
import consulo.project.Project
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInsight.intention.RemoveBracesIntention
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions.ParenthesizedElement.Ops
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.ScParenthesizedElement
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScFunctionalTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameterClause
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaRefactoringUtil.getShortText
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker.checkInspection

abstract class ScalaUnnecessaryParenthesesInspectionBase extends LocalInspectionTool {

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession, state: Object): PsiElementVisitor =
    buildVisitor(holder, isOnTheFly, settingsOf(state))

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    buildVisitor(holder, isOnTheFly, UnnecessaryParenthesesSettings.default)

  protected def settingsOf(state: Object): UnnecessaryParenthesesSettings

  private def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, currentSettings: UnnecessaryParenthesesSettings): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case parenthesized: ScParenthesizedElement if isProblem(parenthesized, currentSettings) =>
      val quickFix = new RemoveParenthesesFix(parenthesized)
      registerProblem(parenthesized, quickFix, holder, isOnTheFly)
    case functionExpr: ScFunctionExpr if hasSingleParenthesizedParam(functionExpr) && !currentSettings.ignoreAroundFunctionExprParam =>
      functionExpr.params.clauses.headOption.foreach { clause =>
        val quickFix = new RemoveParamClauseParenthesesFix(clause)
        registerProblem(clause, quickFix, holder, isOnTheFly)
      }
    case _ =>
  }

  private def hasSingleParenthesizedParam(functionExpr: ScFunctionExpr): Boolean = {
    // In the case of a single untyped formal parameter, (x) => e can be abbreviated to x => e
    def hasNoParamType = functionExpr.parameters.headOption.exists(_.typeElement.isEmpty)

    def hasParenthesizedClause = functionExpr.params.clauses.headOption.exists(_.hasParenthesis)

    functionExpr.parameters.sizeIs == 1 && hasParenthesizedClause && hasNoParamType
  }

  protected[scala] def isParenthesesRedundant(elem: ScParenthesizedElement, settings: UnnecessaryParenthesesSettings): Boolean = {
    if (elem.isParenthesisNeeded) return false

    elem match {
      case _ if elem.isParenthesisClarifying && settings.ignoreClarifying => false
      case ScParenthesizedElement(_: ScFunctionalTypeElement) if settings.ignoreAroundFunctionType => false
      case _ if elem.isFunctionTypeSingleParam && settings.ignoreAroundFunctionTypeParam => false
      case _ => true
    }
  }

  private def isProblem(elem: ScParenthesizedElement, settings: UnnecessaryParenthesesSettings): Boolean =
    !elem.isNestedParenthesis &&
      checkInspection(this, elem) &&
      isParenthesesRedundant(elem, settings)

  private def registerProblem(elt: ScalaPsiElement, qf: LocalQuickFix, holder: ProblemsHolder, isOnTheFly: Boolean): Unit =
    registerRedundantParensProblem(ScalaInspectionBundle.message("displayname.unnecessary.parentheses"), elt, qf, holder, isOnTheFly)
}

private abstract class RemoveParenthesesFixBase[E <: ScalaPsiElement](element: E)
  extends AbstractFixOnPsiElement[E](
    ScalaInspectionBundle.message("remove.unnecessary.parentheses.with.text", getShortText(element)),
    element
  )

private final class RemoveParenthesesFix(parenthesized: ScParenthesizedElement) extends RemoveParenthesesFixBase(parenthesized) {
  override protected def doApplyFix(element: ScParenthesizedElement)(implicit project: Project): Unit = {
    val keepParentheses = element.isNestingParenthesis
    // remove first the duplicate parentheses
    val replaced = element.doStripParentheses(keepParentheses) match {
      // Remove the last level of parentheses if allowed
      case paren: ScParenthesizedElement if paren.isParenthesisRedundant => paren.doStripParentheses()
      case other => other
    }

    element.innerElement
      .map(RemoveBracesIntention.collectComments(_))
      .foreach(RemoveBracesIntention.addComments(_, replaced.getParent, replaced))

    ScalaPsiUtil.padWithWhitespaces(replaced)
  }
}

private final class RemoveParamClauseParenthesesFix(paramClause: ScParameterClause) extends RemoveParenthesesFixBase(paramClause) {
  override protected def doApplyFix(element: ScParameterClause)(implicit project: Project): Unit =
    if (element.hasParenthesis) {
      val node = element.getNode
      node.removeChild(node.getFirstChildNode)
      node.removeChild(node.getLastChildNode)
    }
}
