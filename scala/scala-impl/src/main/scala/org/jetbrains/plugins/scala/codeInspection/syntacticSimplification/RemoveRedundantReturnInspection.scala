package org.jetbrains.plugins.scala.codeInspection.syntacticSimplification

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.quickfix.RemoveReturnKeywordQuickFix
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReturn
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScExpressionExt, ScFunction, ScFunctionDefinition}

@ExtensionImpl
class RemoveRedundantReturnInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.redundant.return"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.syntactic.simplification"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case function: ScFunctionDefinition =>
      for (body <- function.body) {
        val returns = body.calculateTailReturns
        body.depthFirst {
          !_.isInstanceOf[ScFunction]
        }.foreach {
          case r: ScReturn =>
            if (returns.contains(r)) {
              holder.registerProblem(
                r.keyword,
                ScalaInspectionBundle.message("return.keyword.is.redundant"),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                new RemoveReturnKeywordQuickFix(r)
              )
            }
          case _ =>
        }
      }
    case _ =>
  }
}
