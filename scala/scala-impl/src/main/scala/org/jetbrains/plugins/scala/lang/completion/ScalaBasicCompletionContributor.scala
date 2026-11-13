package org.jetbrains.plugins.scala.lang.completion

import consulo.language.editor.completion.CompletionType
import consulo.language.pattern.PlatformPatterns

class ScalaBasicCompletionContributor extends ScalaCompletionContributor {

  extend(
    CompletionType.BASIC,
    PlatformPatterns.psiElement(),
    new ScalaBasicCompletionProvider
  )
}
