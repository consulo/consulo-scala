package org.jetbrains.plugins.scala.worksheet

import consulo.annotation.component.ExtensionImpl
import consulo.virtualFileSystem.fileType.FileType
import org.jetbrains.plugins.scala.editor.todo.ScalaTodoIndexer

// upstream: <todoIndexer filetype="Scala Worksheet" implementationClass="org.jetbrains.plugins.scala.editor.todo.ScalaTodoIndexer"/>
@ExtensionImpl
final class WorksheetTodoIndexer extends ScalaTodoIndexer {
  override def getFileType: FileType = WorksheetFileType
}
