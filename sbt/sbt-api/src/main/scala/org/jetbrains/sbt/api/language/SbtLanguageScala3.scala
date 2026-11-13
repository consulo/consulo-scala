package org.jetbrains.sbt.api.language
import consulo.language.{DependentLanguage, Language}
import consulo.language.file.LanguageFileType

final class SbtLanguageScala3 private
  extends Language(SbtLanguage.INSTANCE, "sbt Scala 3")
    with DependentLanguage {

  override def getAssociatedFileType: LanguageFileType = SbtFileType
}

object SbtLanguageScala3 {
  val INSTANCE = new SbtLanguageScala3
}
