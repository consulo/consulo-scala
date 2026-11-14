package org.jetbrains.plugins.scala.compiler.highlighting.services

import consulo.annotation.component.ServiceImpl
import consulo.application.util.concurrent.AppExecutorUtil
import consulo.document.{Document, FileDocumentManager}
import consulo.language.psi.{PsiFile, PsiManager}
import consulo.logging.Logger
import consulo.project.{DumbService, Project}
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.{Inject, Singleton}
import org.jetbrains.plugins.scala.compiler.highlighting.services.requests.*
import org.jetbrains.plugins.scala.compiler.highlighting.triggers.TriggerUtil
import org.jetbrains.plugins.scala.compiler.highlighting.util.PsiUtil
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.util.control.NonFatal

/**
 * Keeps track of the files that need highlightings, one entry per file, so a newer request replaces an older one.
 * The requests are compiled one by one on a single thread, the most recent first.
 *
 * upstream: a planner decides between serving the recorded diagnostics, an incremental JPS/BSP build of every
 * unbuilt file and a document compilation, against a model of the build (CompilationLogicService), and the requests
 * are run by a debounced runner with tracing. Here every request is a document (or worksheet) compilation against
 * the module output: the output is updated by Build, after which the open editors are compiled again
 * (see [[org.jetbrains.plugins.scala.compiler.highlighting.listeners.CompilerHighlightingCompilationStatusListener]]).
 */
@Singleton
@ServiceImpl
final class CompilerHighlightingService @Inject()(project: Project) extends HighlightingService:

  private val Log = Logger.getInstance(classOf[CompilerHighlightingService])

  private val retryAfterMillis = 250L

  private val held = new AtomicReference(Map.empty[VirtualFile, HighlightingRequest])

  private val running = new AtomicBoolean(false)

  @volatile private var disposed = false

  private val executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Scala compiler based highlighting", 1)

  override def request(reason: String): Unit =
    Log.debug(s"Waking up runner: $reason")
    wakeUp()

  override def request(request: HighlightingRequest): Unit =
    held.updateAndGet(_.updated(request.file, request))
    this.request(request.debugReason)

  override def compile(request: CompilationRequest): Unit = if !project.isDisposed
    then request.execute()
    else Log.warn(s"project disposed, skipped: $request")

  override def dispose(): Unit =
    disposed = true
    held.set(Map.empty)
    ProjectProgressService(project).cancel()

  private def wakeUp(): Unit =
    if !disposed && running.compareAndSet(false, true) then
      executor.execute(() => drain())

  private def drain(): Unit =
    var waiting = false
    try
      var work = true
      while work && !disposed && !project.isDisposed do
        val (next, notReady) = this.next()
        waiting = notReady
        next match
          case Some(request) => compileSafely(request)
          case None => work = false
    finally
      running.set(false)

    if !disposed && !project.isDisposed then
      if waiting then
        // a lookup or a live template is active in the editors of the waiting files, or the project is indexing
        AppExecutorUtil.getAppScheduledExecutorService.schedule((() => wakeUp()): Runnable, retryAfterMillis, TimeUnit.MILLISECONDS)
      else if held.get().nonEmpty then
        wakeUp()

  private def compileSafely(request: CompilationRequest): Unit =
    try
      if !request.isExpired then compile(request)
    catch
      case NonFatal(t) => Log.warn(s"[${project.getName}] compiler based highlighting failed", t)

  /**
   * The most recent request which can be compiled now, and whether there are requests which wait for something.
   * Expired requests (no editor shows the file, compiler highlighting was disabled) are dropped.
   */
  private def next(): (Option[CompilationRequest], Boolean) =
    val requests = held.updateAndGet(_.filter((file, _) => file.isValid)).values.toSeq.sorted
    var notReady = false
    val found = requests.iterator.map { request =>
      readiness(request) match
        case RequestState.Expired =>
          take(request)
          None
        case RequestState.NotReady =>
          notReady = true
          None
        case RequestState.Ready =>
          take(request)
          build(request)
    }.collectFirst { case Some(compilationRequest) => compilationRequest }
    (found, notReady)

  /**
   * Removes the entry of `request`, unless a newer request for the same file arrived meanwhile.
   */
  private def take(request: HighlightingRequest): Unit =
    held.updateAndGet { current =>
      if current.get(request.file).exists(_ eq request) then current - request.file else current
    }

  private def readiness(request: HighlightingRequest): RequestState =
    inReadAction {
      if !TriggerUtil.isHighlightingEnabled(project) then RequestState.Expired
      else if DumbService.isDumb(project) then RequestState.NotReady
      else Option(documentOf(request.file)).fold(RequestState.Expired)(RequestState.of(project, _))
    }

  private def build(request: HighlightingRequest): Option[CompilationRequest] =
    inReadAction {
      val file = request.file
      for
        document <- Option(documentOf(file))
        psiFile <- Option(psiOf(file)) if TriggerUtil.isHighlightingEnabledFor(psiFile, file, project)
        compilationRequest <- psiFile match
          case scalaFile: ScalaFile if PsiUtil.isWorksheet(scalaFile) =>
            Some(new WorksheetRequest(scalaFile, file, document, project))
          case _ =>
            Option(TriggerUtil.moduleFor(project, file)).map { module =>
              DocumentRequest(TriggerUtil.fileCompilationScope(project, file, module, document, psiFile), project)
            }
      yield
        compilationRequest.documentVersions // snapshot the compiled text versions
        compilationRequest
    }

  private def psiOf(file: VirtualFile): PsiFile = PsiManager.getInstance(project).findFile(file)

  private def documentOf(file: VirtualFile): Document = FileDocumentManager.getInstance().getDocument(file)
