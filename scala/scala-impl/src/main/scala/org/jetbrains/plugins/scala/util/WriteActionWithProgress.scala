package org.jetbrains.plugins.scala.util

import consulo.application.concurrent.coroutine.WriteLock
import consulo.application.progress.{EmptyProgressIndicator, ProgressBuilderFactory, ProgressIndicator}
import consulo.application.{Application, WriteAction}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.UIAccess
import consulo.util.concurrent.coroutine.Coroutine
import consulo.util.concurrent.coroutine.Continuation

import java.util.concurrent.CompletableFuture

/**
 * Replacement of IntelliJ internal `ApplicationEx.runWriteActionWithCancellableProgressInDispatchThread`.
 */
object WriteActionWithProgress {
  /**
   * Old (synchronous) style for callers inside a command or working with the editor: the action runs in a plain
   * write action in the current command, without progress UI.
   */
  def runInCommand(action: java.util.function.Consumer[ProgressIndicator]): Boolean = {
    WriteAction.run[RuntimeException](() => action.accept(new EmptyProgressIndicator))
    true
  }

  /**
   * Runs `action` in a [[WriteLock]] coroutine step under a modal cancelable progress ([[ProgressBuilderFactory]]).
   * Asynchronous and outside of any command: code depending on the result has to continue on the returned future.
   */
  def run[T](project: Project, title: String)(action: ProgressIndicator => T): CompletableFuture[T] =
    Application.get().getInstance(classOf[ProgressBuilderFactory])
      .newProgressBuilder(project, LocalizeValue.of(title))
      .cancelable()
      .modal()
      .execute(UIAccess.current(), () => Coroutine.first(WriteLock.apply[Object, T] { (_: Object, continuation: Continuation[?]) =>
        action(ProgressIndicator.from(continuation))
      }))
}
