package org.jetbrains.plugins.scala.markdown

import consulo.annotation.component.ExtensionImpl

import consulo.language.Language
import consulo.markdown.injection.LanguageGuesser
import org.intellij.plugins.markdown.lang.psi.MarkdownCodeFence
import org.jetbrains.plugins.scala.api.Scala3Language

// first: DefaultLanguageGuesser would map "scala" to the Scala 2 language
// order "first" is not allowed on consulo.markdown extension points
@ExtensionImpl
final class ScalaCodeFenceLanguageProvider extends LanguageGuesser {
  override def guessLanguage(markdownCodeFence: MarkdownCodeFence, fenceLanguage: String): Language =
    guessLanguage(fenceLanguage)

  override def guessLanguage(fenceLanguage: String): Language =
    if ("scala".equalsIgnoreCase(LanguageGuesser.getLanguageName(fenceLanguage))) Scala3Language.INSTANCE
    else null
}
