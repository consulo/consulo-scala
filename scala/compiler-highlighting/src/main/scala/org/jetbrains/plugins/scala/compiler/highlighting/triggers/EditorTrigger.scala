package org.jetbrains.plugins.scala.compiler.highlighting.triggers

import consulo.fileEditor.FileEditorManager
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.compiler.highlighting.services.BackgroundExecutorService.executeOnBackgroundThreadInNotDisposed
import org.jetbrains.plugins.scala.compiler.highlighting.services.TriggerService

/**
 * Compiles the file the user has just landed in, either because its editor was opened or because its tab was
 * selected.
 */
private[highlighting] object EditorTrigger:
  def triggerOnFile(project: Project, virtualFile: VirtualFile, debugReason: String): Unit =
    TriggerService(project).bypass(virtualFile, s"[${virtualFile.getName}] $debugReason")

  def triggerOnSelectedEditor(project: Project, debugReason: String): Unit =
    executeOnBackgroundThreadInNotDisposed(project) {
      FileEditorManager.getInstance(project).getSelectedFiles.headOption.foreach { file =>
        triggerOnFile(project, file, debugReason)
      }
    }
