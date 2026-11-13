package org.jetbrains.plugins.scalaDirective.lang.completion

import consulo.language.editor.completion.{CompletionContributor, CompletionType}
import consulo.application.dumb.DumbAware
import org.jetbrains.plugins.scalaDirective.dependencies.ScalaDirectiveDependencyPattern

abstract class ScalaDirectiveDependencyCompletionContributorBase extends CompletionContributor with DumbAware {
  protected def provider: ScalaDirectiveDependencyCompletionProviderBase

  extend(CompletionType.BASIC, ScalaDirectiveDependencyPattern, provider)
}
