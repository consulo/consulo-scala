package org.jetbrains.plugins.scala.markdown

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.completion.CompletionParameters
import consulo.language.editor.completion.lookup.LookupElement
import consulo.language.Language
import org.intellij.plugins.markdown.injection.CodeFenceLanguageProvider
import org.jetbrains.plugins.scala.api.Scala3Language

import java.util.Collections.emptyList
import java.util.List as JList

@ExtensionImpl
final class ScalaCodeFenceLanguageProvider extends CodeFenceLanguageProvider:
  override def getCompletionVariantsForInfoString(parameters: CompletionParameters): JList[LookupElement] = emptyList()

  override def getLanguageByInfoString(infoString: String): Language =
    if "scala".equalsIgnoreCase(infoString) then Scala3Language.INSTANCE
    else null
