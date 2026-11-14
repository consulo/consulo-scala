package org.jetbrains.sbt.language

import consulo.annotation.component.ExtensionImpl
import consulo.virtualFileSystem.fileType.FileType
import org.jetbrains.plugins.scala.editor.todo.ScalaTodoIndexer
import org.jetbrains.sbt.api.language.SbtFileType

// upstream: <todoIndexer filetype="sbt" implementationClass="org.jetbrains.plugins.scala.editor.todo.ScalaTodoIndexer"/>
@ExtensionImpl
final class SbtTodoIndexer extends ScalaTodoIndexer {
  override def getFileType: FileType = SbtFileType
}
