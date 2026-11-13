package org.jetbrains.plugins.scala.util

import consulo.application.WriteAction
import consulo.application.progress.{EmptyProgressIndicator, ProgressIndicator}

import java.util.function.Consumer

/**
 * Replacement of IntelliJ internal `ApplicationEx.runWriteActionWithCancellableProgressInDispatchThread`:
 * Consulo public API has no modal progress for write actions, so the action is executed in a plain write action.
 */
object WriteActionWithProgress {
  def run(action: Consumer[ProgressIndicator]): Boolean = {
    WriteAction.run[RuntimeException](() => action.accept(new EmptyProgressIndicator))
    true
  }
}
