package org.jetbrains.plugins.scala.lang.completion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.completion.SkipAutopopupInStrings
import org.jetbrains.plugins.scala.api.ScalaLanguage

@ExtensionImpl(id = "scalaSkipAutopopupInStrings", order = "after scalaCompletionConfidence")
final class ScalaSkipAutopopupInStrings extends SkipAutopopupInStrings {
  override def getLanguage: Language = ScalaLanguage.INSTANCE
}
