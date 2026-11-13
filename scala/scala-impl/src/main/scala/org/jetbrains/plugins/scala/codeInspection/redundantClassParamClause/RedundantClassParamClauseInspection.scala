package org.jetbrains.plugins.scala.codeInspection.redundantClassParamClause

import consulo.language.editor.inspection.ProblemHighlightType.LIKE_UNUSED_SYMBOL
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass

final class RedundantClassParamClauseInspection extends LocalInspectionTool with DumbAware {
  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case c: ScClass if !c.isCase =>
      for {
        paramClauses <- c.clauses
        if paramClauses.isEmpty && paramClauses.getTextLength > 0
      } {
        holder.registerProblem(
          paramClauses,
          ScalaInspectionBundle.message("empty.parameter.clause.is.redundant"),
          LIKE_UNUSED_SYMBOL,
          new RemoveRedundantClassParamClause(paramClauses)
        )
      }
    case _ =>
  }
}
