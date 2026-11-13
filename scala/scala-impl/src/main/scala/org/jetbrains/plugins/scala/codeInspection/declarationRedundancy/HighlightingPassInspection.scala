package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.language.editor.inspection.{LocalInspectionTool, LocalQuickFix, ProblemsHolder}
import consulo.project.ProjectManager
import consulo.application.util.HtmlChunk
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.incremental
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

trait HighlightingPassInspection extends LocalInspectionTool {
  override final def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = {
    if (isOnTheFly) {
      if (ScalaProjectSettings.in(holder.getProject).isIncrementalHighlighting) buildIncrementalHighlightingVisitor(holder) //highlighting pass is disabled
      else PsiElementVisitor.EMPTY_VISITOR //highlighting pass should take care of that
    }
    else {
      //REMINDER: this is for the case when inspection are run in batch mode
      new PsiElementVisitor {
        override def visitElement(element: PsiElement): Unit = if (shouldProcessElement(element)) {
          invoke(element, isOnTheFly).foreach { info =>
            holder.registerProblem(info.element, info.message, info.fixes*)
          }
        }
      }
    }
  }

  def buildIncrementalHighlightingVisitor(holder: ProblemsHolder): PsiElementVisitor = PsiElementVisitor.EMPTY_VISITOR

  def invoke(element: PsiElement, isOnTheFly: Boolean): Seq[ProblemInfo]

  def shouldProcessElement(elem: PsiElement): Boolean

  def getDescriptionAddendum: HtmlChunk =
    if (ProjectManager.getInstance.getOpenProjects.exists(incremental.Highlighting.enabledIn))
      HtmlChunk.text(ScalaInspectionBundle.message("suppressed.in.incremental.highlighting.mode")).wrapWith("sup").wrapWith("p")
    else
      HtmlChunk.empty
}

case class ProblemInfo(element: PsiElement,
                       @Nls message: String,
                       fixes: Seq[LocalQuickFix])
