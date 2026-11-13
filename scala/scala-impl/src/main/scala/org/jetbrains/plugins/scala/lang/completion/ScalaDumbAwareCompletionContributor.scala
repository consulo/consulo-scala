package org.jetbrains.plugins.scala.lang.completion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.completion.CompletionInitializationContext
import consulo.application.dumb.DumbAware

@ExtensionImpl(id = "scalaDumbAwareCompletionContributor")
final class ScalaDumbAwareCompletionContributor extends ScalaCompletionContributor with DumbAware {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  /**
   * Provide fixed dummy identifier for Scala completion even during indexing
   *
   * NOTE: `context.setDummyIdentifier` should be called only once per language
   * @see [[com.intellij.codeInsight.completion.CompletionInitializationUtil.runContributorsBeforeCompletion]]
  */
  override def beforeCompletion(context: CompletionInitializationContext): Unit = {
    context.setDummyIdentifier(dummyIdentifier(context.getFile, context.getStartOffset - 1))
    super.beforeCompletion(context)
  }
}
