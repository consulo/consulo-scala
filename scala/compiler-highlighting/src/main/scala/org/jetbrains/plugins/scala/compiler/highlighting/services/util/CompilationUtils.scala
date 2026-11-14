package org.jetbrains.plugins.scala.compiler.highlighting.services.util

import consulo.application.progress.{EmptyProgressIndicator, ProgressManager}
import consulo.component.ProcessCanceledException
import consulo.logging.Logger
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.compiler.highlighting.core.CompilerEventGeneratingClient
import org.jetbrains.plugins.scala.compiler.highlighting.services.requests.SerializableMap
import org.jetbrains.plugins.scala.compiler.highlighting.services.{CompilationLogicService, ProjectProgressService}
import org.jetbrains.plugins.scala.shared.util.CanonicalPath

object CompilationUtils {

  private val Log = Logger.getInstance(CompilationUtils.getClass)

  /**
   * upstream: prepareCompilation saves the project and starts the compile server, then the compilation runs in a
   * Task.Backgroundable whose progress is shown in the status bar after a delay, under the compiler lock shared with
   * JPS. Here the in-process compilation runs on the calling thread (the compilation thread of
   * [[org.jetbrains.plugins.scala.compiler.highlighting.services.CompilerHighlightingService]]) without a visible
   * progress: it is cancelled when the project is closed.
   *
   * @param files the files the compilation is for, recorded as compiled for the state the compilation started with
   */
  def performCompilation(
    project: Project,
    files: Set[VirtualFile],
    compiledTextVersions: SerializableMap[CanonicalPath, Long]
  )(compile: CompilerEventGeneratingClient => Unit): Unit = {
    if (project.isDisposed) return

    val indicator = new EmptyProgressIndicator
    val progress = ProjectProgressService(project)
    val logic = CompilationLogicService(project)
    val token = logic.compilationStarted()
    progress.setIndicator(indicator)
    try {
      ProgressManager.getInstance().runProcess((() => {
        val client = new CompilerEventGeneratingClient(project, indicator, Log, compiledTextVersions)
        var interrupted = true
        try {
          compile(client)
          interrupted = false
        } finally {
          if (client.endIfInterrupted()) interrupted = true
        }
        if (!interrupted && !indicator.isCanceled) {
          logic.compilationFinished(token, files)
        }
      }): Runnable, indicator)
    } catch {
      case _: ProcessCanceledException =>
    } finally {
      progress.clearIndicator()
    }
  }
}
