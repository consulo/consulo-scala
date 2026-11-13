package org.jetbrains.plugins.scala.project

import consulo.fileEditor.FileEditorManager
import consulo.virtualFileSystem.fileType.FileTypeRegistry
import consulo.project.Project
import consulo.fileEditor.EditorNotifications
import org.jetbrains.plugins.scala.api.ScalaFileType

object ScalaProjectConfigurationUtil {
  def refreshEditorNotifications(project: Project): Unit = {
    val openFiles = FileEditorManager.getInstance(project).getOpenFiles
    val openScalaFiles = openFiles.filter(FileTypeRegistry.getInstance.isFileOfType(_, ScalaFileType.INSTANCE))
    if (openScalaFiles.isEmpty) return

    val editorNotifications = EditorNotifications.getInstance(project)
    openScalaFiles.foreach(editorNotifications.updateNotifications)
  }
}
