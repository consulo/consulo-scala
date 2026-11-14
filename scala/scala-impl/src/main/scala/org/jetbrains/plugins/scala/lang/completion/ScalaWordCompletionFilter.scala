package org.jetbrains.plugins.scala.lang.completion

import consulo.annotation.component.ExtensionImpl

import consulo.language.Language
import consulo.language.editor.completion.WordCompletionElementFilter
import org.jetbrains.plugins.scala.api.ScalaLanguage

@ExtensionImpl
final class ScalaWordCompletionFilter extends WordCompletionElementFilter {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def isWordCompletionInDumbModeEnabled: Boolean = false
}
