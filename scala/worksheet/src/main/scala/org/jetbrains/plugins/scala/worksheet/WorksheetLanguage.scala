package org.jetbrains.plugins.scala.worksheet

import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

final class WorksheetLanguage private()
  extends Language(ScalaLanguage.INSTANCE, "Scala Worksheet")
    with WorksheetLanguageLike

object WorksheetLanguage {
  final val INSTANCE = new WorksheetLanguage
}