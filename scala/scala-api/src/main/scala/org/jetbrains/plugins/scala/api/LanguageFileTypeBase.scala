package org.jetbrains.plugins.scala.api

import consulo.language.Language
import consulo.language.file.{FileTypeManager, LanguageFileType}
import consulo.localize.LocalizeValue
import consulo.virtualFileSystem.VirtualFile

abstract class LanguageFileTypeBase(language: Language) extends LanguageFileType(language) {

  // upstream: FileType.getName, in Consulo - FileType.getId
  //noinspection ReferencePassedToNls
  override def getId: String = getLanguage.getID

  override def getDefaultExtension: String = getId.toLowerCase

  //noinspection ScalaExtractStringToBundle,ReferencePassedToNls
  override def getDescription: LocalizeValue = LocalizeValue.localizeTODO(getId + " files")

  // TODO Temporary fix for Upsource
  // (FileTypeManagerImpl.isFileOfType is called from ScalaSourceFilterScope,
  //   while FileTypeManager contains no .scala pattern)
  // take into account: SCL-16417
  // TODO maybe remove this method.
  //  It's a legacy of LanguageFileTypeBase inheriting from the FileTypeIdentifiableByVirtualFile
  private[jetbrains] def isMyFileType(file: VirtualFile): Boolean = {
    val ext = file.getExtension
    if (ext == null) return false
    FileTypeManager.getInstance.getFileTypeByExtension(ext) == this
  }
}

