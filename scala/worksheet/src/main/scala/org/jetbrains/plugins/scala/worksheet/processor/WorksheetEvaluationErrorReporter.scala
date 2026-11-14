package org.jetbrains.plugins.scala.worksheet.processor

import consulo.project.ui.notification.NotificationType
import consulo.logging.Logger
import consulo.codeEditor.{Editor, LogicalPosition}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.repl.console.configuration.ScalaSdkJLineFixer
import org.jetbrains.plugins.scala.worksheet.WorksheetBundle
import org.jetbrains.plugins.scala.worksheet.processor.WorksheetCompiler.WorksheetCompilerResult
import org.jetbrains.plugins.scala.worksheet.processor.WorksheetCompiler.WorksheetCompilerResult.WorksheetCompilerError
import org.jetbrains.plugins.scala.worksheet.processor.WorksheetEvaluationErrorReporter.showConfigErrorNotification
import org.jetbrains.plugins.scala.worksheet.server.RemoteServerConnector.RemoteServerConnectorResult
import org.jetbrains.plugins.scala.worksheet.utils.notifications.WorksheetNotificationsGroup

class WorksheetEvaluationErrorReporter(
  project: Project,
  worksheetFile: VirtualFile,
  worksheetEditor: Editor,
  log: Logger
) {

  //noinspection RedundantBlock
  def reportError(error: WorksheetCompilerError): Unit = {
    val WCR = WorksheetCompilerResult
    error match {
      case WCR.PreprocessError(error)            => showCompilationError(error.message, error.position)
      case WCR.UnknownError(exception)           => reportUnexpectedError(exception)
      case WCR.CompilationError                  => // assuming that compilation errors are already reported by WorksheetCompiler
      case WCR.ProcessTerminatedError(_, _)      => // not handled, used to cancel evaluation
      case WCR.ProjectIsAlreadyDisposed(name, trace)  =>
        log.error(s"Project `$name` is already disposed", trace)
      case WCR.RemoteServerConnectorError(error) =>
        val RSCR = RemoteServerConnectorResult
        error match {
          case RSCR.ExpectedError(exception)   =>
            //noinspection ReferencePassedToNls
            showConfigErrorNotification(project, exception.getMessage)
          case RSCR.UnexpectedError(exception) =>
            reportUnexpectedError(exception)
          case RSCR.ProcessTerminatedError(exception) =>
            // upstream: the compile server process terminated (not reported, the stop button stops the whole server),
            // here the in-process compile engine failed
            log.warn(exception)
            //noinspection ReferencePassedToNls
            showConfigErrorNotification(project, Option(exception.getMessage).getOrElse(exception.toString))
          case RSCR.RequiredJLineIsMissingFromClasspathError(module) =>
            ScalaSdkJLineFixer.showJLineMissingNotification(module, WorksheetBundle.message("worksheet.subsystem.name"))
        }
    }
  }

  private def reportUnexpectedError(exception: Throwable): Unit =
    log.error("Unexpected error occurred during worksheet evaluation", exception)

  private def showCompilationError(message: String, position: LogicalPosition): Unit =
    WorksheetCompilerUtil.showCompilationError(
      worksheetFile, position, Array(message),
      () => worksheetEditor.getCaretModel.moveToLogicalPosition(position)
    )(using project)
}

object WorksheetEvaluationErrorReporter {


  def showConfigErrorNotification(project: Project, @Nls msg: String): Unit = {
    if (project.isDisposed)
      return

    WorksheetNotificationsGroup
      .createNotification(
        WorksheetBundle.message("worksheet.configuration.errors.base"),
        msg,
        NotificationType.ERROR,
        null
      )
      .notify(project)
  }
}
