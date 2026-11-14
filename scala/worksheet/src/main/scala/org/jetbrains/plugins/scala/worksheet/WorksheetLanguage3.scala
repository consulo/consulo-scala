package org.jetbrains.plugins.scala.worksheet

import consulo.language.{DependentLanguage, Language}
import consulo.language.file.LanguageFileType
import org.jetbrains.plugins.scala.api.Scala3Language

final class WorksheetLanguage3 private()
  extends Language(Scala3Language.INSTANCE, "Scala 3 Worksheet")
    with DependentLanguage
    with WorksheetLanguageLike {

  /**
   *  no need in overriding for [[WorksheetLanguage]] cause it inferes WorksheetFileType automatically
   * (see super implementation getAssociatedFileType)
   */
  override def getAssociatedFileType: LanguageFileType = WorksheetFileType
}

object WorksheetLanguage3 {
  final val INSTANCE = new WorksheetLanguage3
}
