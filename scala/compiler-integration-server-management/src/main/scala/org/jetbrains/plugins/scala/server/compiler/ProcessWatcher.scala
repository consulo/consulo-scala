package org.jetbrains.plugins.scala.server.compiler
import consulo.process.{ProcessHandler, ProcessOutputTypes}
import consulo.process.event.{ProcessEvent, ProcessListener}
import consulo.application.ApplicationManager
import consulo.logging.Logger
import consulo.project.ProjectManager
import consulo.util.dataholder.Key
import org.jetbrains.jps.incremental.scala.utils.CompileServerSharedMessages
import org.jetbrains.plugins.scala.server.compiler.ProcessWatcher.Log
import org.jetbrains.plugins.scala.extensions.invokeLater

/**
 * @param processHandler the handler of the running Scala Compile Server process (with a blocking output reader),
 *                       its notifications are already started
 * @param local true if the process is running on the same machine where IntelliJ IDEA is running
 */
// upstream: wraps the started java.lang.Process into an OSProcessHandler with a blocking reader
private final class ProcessWatcher(processHandler: ProcessHandler, local: Boolean) {

  @volatile
  private var errorInStdOut = false

  addProcessListener(MyProcessListener)

  def addProcessListener(listener: ProcessListener): Unit =
    processHandler.addProcessListener(listener)

  def running: Boolean = !processHandler.isProcessTerminated

  /**
   * @return the PID of the running Scala Compile Serve process only if the process is running on the same machine
   *         where IntelliJ IDEA is running
   */
  def pid: Option[Long] =
    Option.when(local)(processHandler.getId).filter(_ >= 0)

  def destroyAndWait(): Boolean = {
    processHandler.destroyProcess()
    processHandler.waitFor()
  }

  //true if process exited before timeout
  def destroyAndWaitFor(ms: Long): Boolean = {
    processHandler.destroyProcess()
    processHandler.waitFor(ms)
  }

  private var _terminatedByIdleTimeout = false
  def isTerminatedByIdleTimeout: Boolean = _terminatedByIdleTimeout

  private object MyProcessListener extends ProcessListener {
    override def onTextAvailable(event: ProcessEvent, outputType: Key[?]): Unit = {
      val text = event.getText

      //print(s"[$outputType] $text")
      outputType match {
        case ProcessOutputTypes.STDOUT =>
          if (errorInStdOut || ProcessWatcher.ExceptionPattern.matcher(text).find) {
            errorInStdOut = true
            processErrorText(text, outputType)
          }

          if (text.startsWith(CompileServerSharedMessages.CompileServerShutdownPrefix)) {
            Log.info(s"[$outputType] ${text.stripTrailing()}")
            if (text.contains(CompileServerSharedMessages.ProcessWasIdleFor)) {
              _terminatedByIdleTimeout = true
              invokeLater {
                ProjectManager.getInstance().getOpenProjects.foreach { project =>
                  if (!project.isDisposed) {
                    CompileServerNotifications.showStoppedByIdleTimeoutNotification(project)
                  }
                }
              }
            }
          }

        case ProcessOutputTypes.STDERR =>
          processErrorText(text, outputType)

        case _ => // do nothing
      }
    }

    private def processErrorText(text: String, outputType: Key[?]): Unit = {
      Log.warn(s"[$outputType] ${text.trim}")
      val filtered = text.linesIterator.mkString(System.lineSeparator())
      if (filtered.nonEmpty) {
        ApplicationManager.getApplication.getMessageBus.syncPublisher(CompileServerErrorListener.Topic).onError(filtered)
      }
    }

    override def processTerminated(event: ProcessEvent): Unit = {
      val pidStr = pid.map(p => s"(PID: $p)").getOrElse("(PID not available for remote processes)")
      Log.info(s"compile server process terminated with exit code: ${event.getExitCode} $pidStr")
    }
  }
}

object ProcessWatcher {
  private val Log = Logger.getInstance(classOf[ProcessWatcher])
  private val ExceptionPattern = "[eE]rror|[eE]xception".r.pattern
}
