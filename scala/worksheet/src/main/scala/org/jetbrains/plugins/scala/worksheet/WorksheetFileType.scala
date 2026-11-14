package org.jetbrains.plugins.scala.worksheet

import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import org.jetbrains.plugins.scala.api.LanguageFileTypeBase
import org.jetbrains.plugins.scala.finder.FileTypeWithIsolatedDeclarations
import org.jetbrains.plugins.scala.icons.Icons

object WorksheetFileType
  extends LanguageFileTypeBase(WorksheetLanguage.INSTANCE)
    with FileTypeWithIsolatedDeclarations {

  override def getDefaultExtension = "sc"

  override def getDescription: LocalizeValue = LocalizeValue.of(WorksheetBundle.message("worksheet.file.type.description"))

  // upstream: TODO worksheet logo (the Scala file icon)
  override def getIcon: Image = Icons.SCALA_FILE
}
