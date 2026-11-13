package org.jetbrains.plugins.scala.lang.completion

import consulo.language.editor.completion.DefaultWordCompletionFilter

final class ScalaWordCompletionFilter extends DefaultWordCompletionFilter {
  override def isWordCompletionInDumbModeEnabled: Boolean = false
}
