package org.jetbrains.plugins.scala.actions.utils

import consulo.disposer.Disposable
import consulo.ui.ex.action.AnAction
import consulo.application.{Application, NonBlockingReadAction, ReadAction}
import consulo.codeEditor.{Editor, EditorPopupHelper}
import consulo.codeEditor.util.EditorUtil
import consulo.project.Project
import consulo.ui.ex.awt.LoadingDecorator
import consulo.ui.ex.popup.JBPopupFactory
import consulo.disposer.Disposer
import consulo.ui.ex.awt.NonOpaquePanel
import consulo.ui.ex.awt.{JBLabel, JBLoadingPanel}
import consulo.application.util.concurrent.AppExecutorUtil
import consulo.ui.ex.awt.{AsyncProcessIcon, UIUtil}
import consulo.ui.image.Image
import consulo.util.concurrent.CancellablePromise
import org.jetbrains.annotations.Nullable

import java.awt.FlowLayout
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import javax.swing.{JLabel, JPanel}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps

/**
 * This utility class is designed to be able to run a composite action that:
 *  1. Launches a background task in a non blockable read action with some computation
 *  1. Shows a progress popup while the computation is running
 *  1. Present the computation results on the UI thread
 *
 * The trickiest part is the logic around showing the loading placeholder tooltip and handling cancellations.
 * Without this requirement we could simply use the API in [[com.intellij.openapi.application.ReadAction]]
 * and [[com.intellij.openapi.application.NonBlockingReadAction]]
 *
 * Copied from [[com.intellij.codeInsight.hint.ParameterInfoTaskRunnerUtil]] and converted to Scala.
 * No major significant changes were made in the initial commit.
 */
// upstream: @ApiStatus.Experimental
private[actions] object TaskRunnerWithLoadingProgress {
  private val DefaultProgressPopupDelay: Duration = 1000.millis

  /**
   * @param originalAction an action within which the async task is running.
   *                       If the action is triggered once again, the previous task will be canceled
   *                       (via NonBlockingReadAction.coalesceBy)
   */
  def runSingleInstanceActionTask[T](
    project: Project,
    backgroundDataSupplier: () => T,
    uiDataConsumer: T => Unit,
    @Nullable progressTitle: String,
    editor: Editor,
    cancelOnScrolling: Boolean,
    originalAction: AnAction
  ): CancellablePromise[T] = {
    val backgroundAction = ReadAction
      .nonBlocking[T](() => {
        backgroundDataSupplier()
      })
      .coalesceBy(originalAction)

    TaskRunnerWithLoadingProgress.runTask(
      project = project,
      backgroundAction = backgroundAction,
      uiDataConsumer = (result: T) => {
        // Handling on UI can also throw exceptions, so handle it here as well
        val tri = Try {
          uiDataConsumer(result)
        }

        // Invoke the action completed only when the UI consumer is finished
        // NOTE: we can't use org.jetbrains.concurrency.CancellablePromise.onSuccess as it's invoked before the
        // NonBlockingReadAction.finishOnUiThread callback
        notifyActionCompleted(project, originalAction, tri)
      },
      progressTitle = progressTitle,
      editor = editor,
      cancelOnScrolling = cancelOnScrolling
    ).onError(e => {
      notifyActionCompleted(project, originalAction, scala.util.Failure(e))
    })
  }

  private def notifyActionCompleted(project: Project, originalAction: AnAction, result: Try[Unit]): Unit = {
    val listeners = project.getMessageBus.syncPublisher(ScalaAsyncActionListener.Topic)
    listeners.actionCompleted(originalAction.getClass, result)
  }

  /**
   * @param progressTitle     null means no loading panel should be shown
   * @param cancelOnScrolling cancel execution on scrolling
   */
  def runTask[T](
    project: Project,
    backgroundAction: NonBlockingReadAction[T],
    uiDataConsumer: Consumer[? >: T],
    @Nullable progressTitle: String,
    editor: Editor,
    cancelOnScrolling: Boolean
  ): CancellablePromise[T] = {
    // upstream: used `EditorImpl.getDisposable` (or the project as a fallback)
    val editorOrProjectDisposable = Disposable.newDisposable("TaskRunnerWithLoadingProgress")
    EditorUtil.disposeWithEditor(editor, editorOrProjectDisposable)

    val cancellablePromiseRef = new AtomicReference[CancellablePromise[?]]()
    val (stopAction, stopActionDisposable) =
      startProgressAndCreateStopAction(project, progressTitle, cancellablePromiseRef, editor, editorOrProjectDisposable)

    val visibleAreaListener = new CancelProgressOnScrolling(cancellablePromiseRef)
    if (cancelOnScrolling) {
      editor.getScrollingModel.addVisibleAreaListener(visibleAreaListener)
    }

    val submittedActionPromise = backgroundAction
      .finishOnUiThread(
        (app: Application) => app.getDefaultModalityState,
        result => {
          uiDataConsumer.accept(result)
        }
      )
      .expireWith(editorOrProjectDisposable)
      // If there is a progress tooltip, expire the task if we cancel it (via Escape key or on focus change)
      .pipe(action => stopActionDisposable.fold(action)(action.expireWith))
      .submit(AppExecutorUtil.getAppExecutorService)
      .onProcessed(_ => {
        stopAction.accept(false)
        if (cancelOnScrolling) {
          editor.getScrollingModel.removeVisibleAreaListener(visibleAreaListener)
        }
        UIUtil.invokeLaterIfNeeded(() => Disposer.dispose(editorOrProjectDisposable))
      })

    cancellablePromiseRef.set(submittedActionPromise)
    submittedActionPromise
  }

  /**
   * @note Again, the implementation is copied from `com.intellij.codeInsight.hint.ParameterInfoTaskRunnerUtil`
   */
  private def startProgressAndCreateStopAction(
    project: Project,
    @Nullable progressTitle: String,
    promiseRef: AtomicReference[? <: CancellablePromise[?]],
    editor: Editor,
    editorOrProjectDisposable: Disposable
  ): (Consumer[Boolean], Option[Disposable]) = {
    val stopActionRef = new AtomicReference[Consumer[Boolean]]

    val originalStopAction: Consumer[Boolean] = (cancel: Boolean) => {
      stopActionRef.set(null)

      if (cancel) {
        val promise = promiseRef.get()
        if (promise != null) {
          promise.cancel()
        }
      }
    }

    if (progressTitle == null) {
      stopActionRef.set(originalStopAction)
      (stopActionRef.get(), None)
    } else {
      val disposable = Disposable.newDisposable()
      Disposer.register(editorOrProjectDisposable, disposable)

      val loadingPanel = new JBLoadingPanel(null, panel =>
        new LoadingDecorator(panel, disposable, 0, false, new AsyncProcessIcon(s"ProgressUtils(title: $progressTitle)")) {
          override protected def customizeLoadingLayer(parent: JPanel, text: JLabel, icon: AsyncProcessIcon): NonOpaquePanel = {
            parent.setLayout(new FlowLayout(FlowLayout.LEFT))
            val result = new NonOpaquePanel
            result.add(icon)
            parent.add(result)
            result
          }
        })
      loadingPanel.add(new JBLabel(Image.empty(18)))
      loadingPanel.add(new JBLabel(progressTitle))

      val popupBuilder = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(loadingPanel, null)
        .setProject(project)
        .setCancelCallback(() => {
          val stopAction = stopActionRef.get()
          if (stopAction != null) {
            stopAction.accept(true)
          }
          true
        })

      val popup = popupBuilder.createPopup()
      Disposer.register(disposable, popup)

      // upstream: EdtScheduler.schedule(delay, ModalityState.defaultModalityState(), ...)
      val application = Application.get()
      val showPopupFuture = AppExecutorUtil.getAppScheduledExecutorService.schedule(
        (() => application.invokeLater(() => {
          val needToShowPopup = !popup.isDisposed && !popup.isVisible && !editor.isDisposed
          if (needToShowPopup) {
            loadingPanel.startLoading()
            EditorPopupHelper.getInstance.showPopupInBestPositionFor(editor, popup)
          }
        }, application.getDefaultModalityState)): Runnable,
        DefaultProgressPopupDelay.toMillis,
        TimeUnit.MILLISECONDS
      )

      stopActionRef.set((cancel: Boolean) => {
        try {
          loadingPanel.stopLoading()
          originalStopAction.accept(cancel)
        } finally {
          showPopupFuture.cancel(false)

          UIUtil.invokeLaterIfNeeded(() => {
            if (popup.isVisible) {
              popup.setUiVisible(false)
            }
            Disposer.dispose(disposable)
          })
        }
      })

      (stopActionRef.get(), Some(disposable))
    }
  }
}