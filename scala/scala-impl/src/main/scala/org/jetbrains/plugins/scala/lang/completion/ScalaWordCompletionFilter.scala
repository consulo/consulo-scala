package org.jetbrains.plugins.scala.lang.completion

import consulo.language.Language
import consulo.language.editor.completion.WordCompletionElementFilter
import org.jetbrains.plugins.scala.api.ScalaLanguage

// upstream: isWordCompletionInDumbModeEnabled = false (no Consulo counterpart)
final class ScalaWordCompletionFilter extends WordCompletionElementFilter {
  override def getLanguage: Language = ScalaLanguage.INSTANCE
}
