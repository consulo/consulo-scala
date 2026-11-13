package org.jetbrains.plugins.scala.codeInspection.infiniteCycle

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import consulo.language.psi.{PsiElement, PsiElementVisitor, PsiReference}
import org.jetbrains.plugins.scala.codeInspection.PsiElementVisitorSimple
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariable

@ExtensionImpl(id = "LoopVariableNotUpdatedInspection")
class LoopVariableNotUpdatedInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.loop.variable.not.updated.inside.loop"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  private val ComparisonOperators = Set("==", "!=", ">", "<", ">=", "<=")

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case ScWhile(
      Some(ScInfixExpr((ref: ScReferenceExpression) & ResolvesTo(target@Parent(Parent(_: ScVariable))), ElementText(operator), _)),
      Some(body)) if !ref.isQualified && ComparisonOperators.contains(operator) && !isMutatedWithing(body, target) =>
        holder.registerProblem(ref.asInstanceOf[PsiReference],
          getDisplayName.get, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    case _ =>
  }

  private def isMutatedWithing(scope: ScalaPsiElement, target: PsiElement): Boolean = {
    val Target = target

    scope.breadthFirst().exists {
      case ScAssignment(_, _) => true
      case e@ScInfixExpr(ResolvesTo(Target), _, _) if e.isAssignmentOperator => true
      case _ => false
    }
  }
}
