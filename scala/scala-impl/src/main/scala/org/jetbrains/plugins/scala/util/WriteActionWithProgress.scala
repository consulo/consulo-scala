package org.jetbrains.plugins.scala.util

import consulo.application.Application
import consulo.application.concurrent.coroutine.WriteLock
import consulo.application.progress.{ProgressBuilderFactory, ProgressIndicator}
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.UIAccess
import consulo.util.concurrent.coroutine.Coroutine
import consulo.util.concurrent.coroutine.Continuation

import java.util.concurrent.CompletableFuture

/**
 * Replacement of IntelliJ internal `ApplicationEx.runWriteActionWithCancellableProgressInDispatchThread`:
 * runs `action` in a [[WriteLock]] coroutine step under a modal cancelable progress ([[ProgressBuilderFactory]]).
 *
 * Unlike upstream it is asynchronous: code depending on the result has to continue on the returned future.
 */
object WriteActionWithProgress {
  def run[T](project: Project, title: String)(action: ProgressIndicator => T): CompletableFuture[T] =
    Application.get().getInstance(classOf[ProgressBuilderFactory])
      .newProgressBuilder(project, LocalizeValue.of(title))
      .cancelable()
      .modal()
      .execute(UIAccess.current(), () => Coroutine.first(WriteLock.apply[Object, T] { (_: Object, continuation: Continuation[?]) =>
        action(ProgressIndicator.from(continuation))
      }))
}
