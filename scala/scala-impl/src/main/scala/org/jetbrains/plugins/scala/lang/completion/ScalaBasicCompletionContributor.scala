package org.jetbrains.plugins.scala.lang.completion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.completion.CompletionType
import consulo.language.pattern.PlatformPatterns

@ExtensionImpl(id = "scalaCompletionContrubutor")
class ScalaBasicCompletionContributor extends ScalaCompletionContributor {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  extend(
    CompletionType.BASIC,
    PlatformPatterns.psiElement(),
    new ScalaBasicCompletionProvider
  )
}
