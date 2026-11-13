package org.jetbrains.plugins.scala.tasty

import consulo.localize.LocalizeValue

import consulo.ui.image.Image
import consulo.virtualFileSystem.fileType.FileType
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.api.ScalaFileType


object TastyFileType extends FileType {
  override def getId: String = tastyName

  override def getDescription: LocalizeValue = LocalizeValue.of(tastyName)

  override def getDefaultExtension: String = "tasty"

  //simply use the same icon which is used by ScalaFileType
  override def getIcon: Image = ScalaFileType.INSTANCE.getIcon

  override def isBinary = true

  override def isReadOnly = true

  override def getCharset(file: VirtualFile, content: Array[Byte]): String = null
}