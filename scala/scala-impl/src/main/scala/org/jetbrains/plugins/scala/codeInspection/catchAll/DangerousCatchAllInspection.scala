package org.jetbrains.plugins.scala.codeInspection.catchAll

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, LocalQuickFix, ProblemHighlightType, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.incremental.Highlighting._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.{ScReferencePattern, ScWildcardPattern}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScBlockExpr, ScCatchBlock}

@ExtensionImpl
final class DangerousCatchAllInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.missing.type.annotation"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = {
    new ScalaElementVisitor {
      override def visitCatchBlock(catchBlock: ScCatchBlock): Unit = {
        if (!catchBlock.isVisible(holder.getProject, holder.getFile)) return

        catchBlock.expression match {
          case Some(block: ScBlockExpr) =>
            for {
              caseClauses <- block.caseClauses
              if caseClauses.caseClauses.sizeIs == 1
              caseClause <- caseClauses.caseClauses.headOption
              pattern <- caseClause.pattern
              if pattern.is[ScWildcardPattern, ScReferencePattern] && caseClause.guard.isEmpty
              fix = (new ReplaceDangerousCatchAllQuickFix(caseClause))
            } holder.registerProblem(holder.getManager.createProblemDescriptor(caseClause.getFirstChild, pattern,
              ScalaInspectionBundle.message("catch.all"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly,
              fix))
          case _ =>
        }
      }
    }
  }
}
