package org.jetbrains.sbt.api.language
import org.jetbrains.plugins.scala.api.LanguageFileTypeBase
import consulo.ui.image.Image
import org.jetbrains.sbt.api.icons.Icons

object SbtFileType extends LanguageFileTypeBase(SbtLanguage.INSTANCE) {
  override def getIcon: Image = Icons.SBT_FILE
}
