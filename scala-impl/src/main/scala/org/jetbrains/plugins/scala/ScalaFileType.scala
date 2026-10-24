package org.jetbrains.plugins.scala

import consulo.language.file.LanguageFileType
import consulo.localize.LocalizeValue
import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.image.Image

/**
 * @author VISTALL
 * @since 2026-10-24
 */
object ScalaFileType extends LanguageFileType(ScalaLanguage):
  val DefaultExtension = "scala"

  override def getId: String = "Scala"

  override def getDescription: LocalizeValue = LocalizeValue.localizeTODO("Scala files")

  override def getDefaultExtension: String = DefaultExtension

  // TODO scala icon (generate-icon)
  override def getIcon: Image = PlatformIconGroup.filetypesAny_type()
