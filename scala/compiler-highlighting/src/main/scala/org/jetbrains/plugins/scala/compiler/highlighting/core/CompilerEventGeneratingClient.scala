package org.jetbrains.plugins.scala.compiler.highlighting.core

import consulo.application.progress.ProgressIndicator
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.project.Project
import org.jetbrains.annotations.Nls
import org.jetbrains.jps.incremental.scala.shared.remote.SerializablePath
import org.jetbrains.jps.incremental.scala.shared.{Client, DummyClient, MessageKind}
import org.jetbrains.plugins.scala.compiler.highlighting.listeners.UpdateCompilerGeneratedStateListener
import org.jetbrains.plugins.scala.compiler.highlighting.services.requests.SerializableMap
import org.jetbrains.plugins.scala.compiler.highlighting.util.CompilerHighlightingBundle
import org.jetbrains.plugins.scala.shared.compiler.CompilerEvent
import org.jetbrains.plugins.scala.shared.util.{CanonicalPath, CompilationId}

import java.nio.file.Path

/**
 * upstream: publishes the compiler events to the CompilerEventListener topic (compiler-integration), the compile
 * server and JPS builds report through it too. Here only the in-process document compilations of this module report
 * compiler events, so they are passed to [[UpdateCompilerGeneratedStateListener]] directly.
 */
private[highlighting] class CompilerEventGeneratingClient(
  project: Project,
  indicator: ProgressIndicator,
  log: Logger,
  compiledTextVersions: SerializableMap[CanonicalPath, Long]
) extends DummyClient {

  final val compilationId = CompilationId(timestamp = System.nanoTime(), documentVersions = compiledTextVersions)

  private val listener = new UpdateCompilerGeneratedStateListener(project)

  private var hasErrors: Boolean = false

  private var compiled: Set[Path] = Set.empty

  private var reported: Set[Path] = Set.empty

  private var errored: Set[Path] = Set.empty

  private var started: Boolean = false

  private var ended: Boolean = false

  indicator.setIndeterminate(false)

  override def progress(@Nls text: String, done: Option[Float]): Unit = {
    indicator.setText(LocalizeValue.of(CompilerHighlightingBundle.message("highlighting.compilation.progress", text)))
    indicator.setFraction(done.getOrElse(-1.0F).toDouble)
    done.foreach { doneVal =>
      sendEvent(CompilerEvent.ProgressEmitted(compilationId, None, doneVal))
    }
  }

  override def message(msg: Client.ClientMsg): Unit = {
    if (msg.text ne null) {
      // TODO: SCL-22142 Revisit after discussing the changes to fatal warnings in Scala 3.4.1 with the compiler team.
      val description = CompilerMessages.description(msg.text)
      if (CompilerMessages.isNoWarningsCanBeIncurred(description)) {
        return
      }
    }

    msg.kind match {
      case MessageKind.Error | MessageKind.InternalBuilderError =>
        hasErrors = true
        msg.source.foreach(source => errored += source.toPath)
      case _ =>
    }
    // Recorded here rather than at `compilationEnd` so that it matches exactly what the store takes from
    // this message: a file the store holds a diagnostic for is a file this compilation reported on.
    msg.source.foreach(source => reported += source.toPath)
    sendEvent(CompilerEvent.MessageEmitted(compilationId, None, None, msg))
  }

  override def compilationStart(): Unit = {
    started = true
    sendEvent(CompilerEvent.CompilationStarted(compilationId, None, None, None))
  }

  // upstream: also refreshes the output roots in the VFS for incremental compilations, document compilations don't
  // write to the module output
  override def compilationEnd(sources: Set[Path]): Unit = {
    ended = true
    compiled = sources
    sendEvent(CompilerEvent.CompilationFinished(compilationId, None, sources.map(SerializablePath(_, pathTranslator))))
  }

  override def isCanceled: Boolean = indicator.isCanceled

  override def trace(exception: Throwable): Unit =
    log.error(s"[${project.getName}] ${exception.getMessage}", exception)

  override def internalInfo(text: String): Unit = log.debug(text)

  override def internalDebug(text: String): Unit = log.debug(text)

  def successful: Boolean = !hasErrors

  /**
   * The sources the compiler reported an error against.
   */
  def erroredSources: Set[Path] = errored

  /**
   * Ends a compilation that started but never ended, and returns whether it had to.
   *
   * Publishing `CompilationFinished` here runs the same cleanup a normal end runs, and the result tells the caller
   * not to treat the compilation as a success or a failure.
   */
  def endIfInterrupted(): Boolean = {
    val interrupted = started && !ended
    if (interrupted) {
      ended = true
      sendEvent(CompilerEvent.CompilationFinished(compilationId, None, Set.empty))
    }
    interrupted
  }

  /**
   * The sources this compilation looked at: the sources `compilationEnd` names plus the files the compiler reported a
   * diagnostic against.
   */
  def compiledSources: Set[Path] = compiled ++ reported

  private def sendEvent(event: CompilerEvent): Unit =
    if (!project.isDisposed) listener.eventReceived(event)
}
