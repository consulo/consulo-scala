package org.jetbrains.plugins.scala.lang.completion

import consulo.language.editor.completion.CompletionInitializationContext
import consulo.application.dumb.DumbAware

final class ScalaDumbAwareCompletionContributor extends ScalaCompletionContributor with DumbAware {

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
