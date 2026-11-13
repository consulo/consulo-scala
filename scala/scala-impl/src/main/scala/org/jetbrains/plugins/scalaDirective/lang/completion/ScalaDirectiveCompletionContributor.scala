package org.jetbrains.plugins.scalaDirective.lang.completion

import consulo.annotation.component.ExtensionImpl

import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage
import consulo.language.editor.completion.{CompletionInitializationContext, CompletionParameters, CompletionProvider, CompletionResultSet, CompletionType}
import consulo.application.dumb.DumbAware
import consulo.language.pattern.PlatformPatterns.psiComment
import consulo.language.pattern.StandardPatterns
import consulo.language.util.ProcessingContext
import org.jetbrains.plugins.scala.lang.completion.{ScalaCompletionContributor, positionFromParameters}
import org.jetbrains.plugins.scalaDirective.lang.completion.lookups.ScalaDirectiveLookupItem

@ExtensionImpl(id = "scalaDirectiveCompletionContributor")
final class ScalaDirectiveCompletionContributor extends ScalaCompletionContributor with DumbAware {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  private val pattern = psiComment.withText(StandardPatterns.string.startsWith(DirectivePrefix))

  extend(CompletionType.BASIC, pattern, new CompletionProvider {
    override def addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet): Unit = {
      val place = positionFromParameters(using parameters)
      val trimmedText = place.getText
        .stripPrefix(DirectivePrefix)
        .replace(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED, "")
        .trim

      if (UsingDirective.startsWith(trimmedText)) {
        result.addElement(ScalaDirectiveLookupItem(UsingDirective))
      }
    }
  })
}
