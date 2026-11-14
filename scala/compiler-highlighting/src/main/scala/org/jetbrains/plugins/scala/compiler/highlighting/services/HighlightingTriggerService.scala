package org.jetbrains.plugins.scala.compiler.highlighting.services

import consulo.annotation.component.ServiceImpl
import consulo.application.ReadAction
import consulo.application.util.concurrent.AppExecutorUtil
import consulo.disposer.Disposable
import consulo.document.{Document, FileDocumentManager}
import consulo.language.file.inject.VirtualFileWindow
import consulo.language.psi.{PsiFile, PsiManager}
import consulo.logging.Logger
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.{Inject, Singleton}
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.compiler.highlighting.services.BackgroundExecutorService.executeOnBackgroundThreadInNotDisposed
import org.jetbrains.plugins.scala.compiler.highlighting.services.core.ScheduledDebouncer
import org.jetbrains.plugins.scala.compiler.highlighting.services.requests.HighlightingRequest
import org.jetbrains.plugins.scala.compiler.highlighting.triggers.TriggerUtil
import org.jetbrains.plugins.scala.compiler.highlighting.util.PsiUtil
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode

import scala.concurrent.duration.FiniteDuration

/**
 * Where every trigger arrives, and the only thing any of them does.
 *
 * A trigger names a file and says why. It does not decide what that file needs: that is settled later,
 * when the scheduler asks, against the state as it is then. So this is deliberately cheap, and no trigger
 * is more meaningful than another.
 */
@Singleton
@ServiceImpl
final class HighlightingTriggerService @Inject()(project: Project) extends TriggerService with Disposable:

  /**
   * The quiet period a file needs before its trigger is handed over.
   *
   * This is the user's compiler highlighting delay, because this is the timer that says:
   * how long after the last edit to react.
   */
  private[highlighting] def debounceDelay: FiniteDuration = ScalaHighlightingMode.compilationDelay(project)

  private val debouncer = new ScheduledDebouncer[VirtualFile](
    debounceDelay,
    AppExecutorUtil.createBoundedScheduledExecutorService("HighlightingTriggerDebouncer", 1),
    this
  )
  private val Log = Logger.getInstance(classOf[HighlightingTriggerService])

  def schedule(@Nullable file: VirtualFile, reason: String): Unit =
    if file eq null then return
    if canBeScheduled(file) then debouncer.queue(file)(handOver(file, reason))

  def bypass(@Nullable file: VirtualFile, reason: String): Unit =
    if file eq null then return
    debouncer.cancel(file)
    if canBeScheduled(file) then handOver(file, reason)

  override def dispose(): Unit = ()

  private[services] def processTrigger(file: VirtualFile, reason: String): Unit =
    if canBeScheduled(file) then requestHighlighting(file, reason)

  /**
   * Looks the file up in a non-blocking read action, which a write action interrupts and restarts. So it only
   * reads: rendering and dispatching happen after it, once.
   */
  private def requestHighlighting(file: VirtualFile, reason: String): Unit =
    Log.debug(s"decision lookup for: $reason")
    // Whether the file is served from the store; None when it cannot be highlighted.
    val servedFromStore = ReadAction.nonBlocking[Option[Boolean]](() =>
      val psi = PsiManager.getInstance(project).findFile(file)
      val doc = FileDocumentManager.getInstance().getDocument(file)
      Option.when(canHighlight(file, psi, doc))(!PsiUtil.isWorksheet(psi) && CompilationLogicService(project).isRecorded(project, file))
    )
      .expireWhen(() => project.isDisposed)
      .executeSynchronously()

    servedFromStore match
      case Some(true) =>
        ExternalHighlightersService(project).renderRecordedDiagnostics(Set(file))
        ()
      case Some(false) =>
        HighlightingService(project).request(HighlightingRequest(file, reason))
      case None =>
        Log.debug(s"not eligible for compilation: $reason")

  private def handOver(file: VirtualFile, reason: String): Unit = executeOnBackgroundThreadInNotDisposed(project):
    processTrigger(file, reason)

  /**
   * Cheap checks that need no read action
   */
  private def canBeScheduled(file: VirtualFile): Boolean =
    !project.isDisposed &&
      TriggerUtil.isHighlightingEnabled(project) &&
      file.isValid &&
      !file.isInstanceOf[VirtualFileWindow]

  /**
   *  Read action needed
   */
  private def canHighlight(virtualFile: VirtualFile, psi: PsiFile, doc: Document) =
    (psi ne null) && (doc ne null) && TriggerUtil.isHighlightingEnabledFor(psi, virtualFile, project)
