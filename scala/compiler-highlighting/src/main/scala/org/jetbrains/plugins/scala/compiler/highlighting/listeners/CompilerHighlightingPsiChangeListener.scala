package org.jetbrains.plugins.scala.compiler.highlighting.listeners

import consulo.language.psi.event.{PsiTreeChangeAdapter, PsiTreeChangeEvent}
import consulo.language.psi.{PsiFile, PsiManager}
import consulo.module.content.ProjectFileIndex
import consulo.project.Project
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.compiler.highlighting.services.CompilationLogicService
import org.jetbrains.plugins.scala.compiler.highlighting.triggers.FileTrigger

/**
 * upstream: registered by the psi.treeChangeListener extension point, here by
 * [[CompilerHighlightingStartupActivity]]
 */
private[highlighting] class CompilerHighlightingPsiChangeListener(project: Project)
  extends PsiTreeChangeAdapter:

  override def childrenChanged(event: PsiTreeChangeEvent): Unit =
    triggerOnFileChange(event.getFile)

  override def childRemoved(event: PsiTreeChangeEvent): Unit =
    if (event.getFile eq null) {
      val child = event.getChild
      child match {
        case null => ()
        case _ => triggerOnFileChange(child.getContainingFile)
      }
    }

  // upstream: SourceRole decides whether the change is of a compiled source or of another build input (Kotlin,
  // Twirl templates), here every change in the source content invalidates the recorded diagnostics
  private def triggerOnFileChange(@Nullable psiFile: PsiFile): Unit =
    if ((psiFile ne null) && !project.isDisposed) {
      val virtualFile = psiFile.getVirtualFile
      if (virtualFile ne null) {
        if (ProjectFileIndex.getInstance(project).isInSourceContent(virtualFile)) {
          CompilationLogicService(project).fileModified(virtualFile)
        }
        FileTrigger.trigger(project, virtualFile, "psi change")
      }
    }

private[highlighting] object CompilerHighlightingPsiChangeListener:
  def register(project: Project): Unit =
    PsiManager.getInstance(project).addPsiTreeChangeListener(new CompilerHighlightingPsiChangeListener(project), project)
