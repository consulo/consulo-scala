package org.jetbrains.plugins.scala.compiler.highlighting.listeners

import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.fileEditor.FileEditorManager
import consulo.fileEditor.event.{FileEditorManagerEvent, FileEditorManagerListener}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.Inject
import org.jetbrains.plugins.scala.compiler.highlighting.triggers.EditorTrigger

/**
 * Starts compiler highlighting for the file the user has just landed in.
 *
 * Both callbacks are needed and neither implies the other: a file can be opened into a split without
 * becoming the selection, and switching tabs selects a file that was opened long ago.
 */
@TopicImpl(ComponentScope.PROJECT)
final class CompilerHighlightingFileEditorListener @Inject()(project: Project) extends FileEditorManagerListener {

  override def fileOpened(source: FileEditorManager, file: VirtualFile): Unit = {
    activated(file, "editor opened")
  }

  override def selectionChanged(event: FileEditorManagerEvent): Unit = {
    Option(event.getNewFile).foreach(activated(_, "editor selected"))
  }

  private def activated(file: VirtualFile, source: String): Unit = {
    if (!project.isDisposed) EditorTrigger.triggerOnFile(project, file, source)
  }
}
