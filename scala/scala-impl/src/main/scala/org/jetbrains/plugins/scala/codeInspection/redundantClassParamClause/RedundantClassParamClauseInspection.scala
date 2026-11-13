package org.jetbrains.plugins.scala.codeInspection.redundantClassParamClause

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.ProblemHighlightType.LIKE_UNUSED_SYMBOL
import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass

@ExtensionImpl(id = "RedundantClassParam")
final class RedundantClassParamClauseInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.redundant.class.param.clause"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WEAK_WARNING

  override def getShortName: String = "RedundantClassParam"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

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
