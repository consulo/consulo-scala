package org.jetbrains.plugins.scala
package worksheet.processor

import consulo.application.ApplicationManager
import consulo.application.progress.{ProgressIndicator, ProgressManager, Task}
import consulo.codeEditor.Editor
import consulo.compiler.{CompilerMessage, CompilerMessageCategory}
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.util.ModuleUtilCore
import consulo.logging.Logger
import consulo.module.Module
import consulo.project.Project
import consulo.ui.annotation.{RequiredUIAccess => RequiresEdt}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.extensions.{LoggerExt, PathExt}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.project.{ModuleExt, ScalaSdkNotConfiguredException}
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.worksheet.processor.WorksheetCompilerUtil.WorksheetCompileRunRequest
import org.jetbrains.plugins.scala.worksheet.processor.WorksheetCompilerUtil.WorksheetCompileRunRequest._
import org.jetbrains.plugins.scala.worksheet.runconfiguration.WorksheetCache
import org.jetbrains.plugins.scala.worksheet.server.RemoteServerConnector.Args.{PlainModeArgs, ReplModeArgs}
import org.jetbrains.plugins.scala.worksheet.server.RemoteServerConnector._
import org.jetbrains.plugins.scala.worksheet.server._
import org.jetbrains.plugins.scala.worksheet.settings.WorksheetExternalRunType.WorksheetPreprocessError
import org.jetbrains.plugins.scala.worksheet.settings._
import org.jetbrains.plugins.scala.worksheet.ui.printers.{WorksheetEditorPrinter, WorksheetEditorPrinterFactory, WorksheetEditorPrinterRepl}
import org.jetbrains.plugins.scala.worksheet.{WorksheetBundle, WorksheetUtils}

import java.nio.file.Files
import scala.collection.mutable
import scala.util.control.NonFatal

//noinspection ScalaWrongPlatformMethodsUsage
// TODO 1: rename/refactor, the class has more responsibilities then to "Compile"
// TODO 2: forbid plugin unloading if some worksheet is running using: DynamicPluginListener.checkUnloadPlugin
//private[worksheet]
class WorksheetCompiler(
  module: Module,
  worksheetFile: ScalaFile
) {

  import worksheet.processor.WorksheetCompiler._

  private implicit val project: Project = worksheetFile.getProject

  private val runType: WorksheetExternalRunType = WorksheetFileSettings(worksheetFile).getRunType
  private val makeType: WorksheetMakeType = WorksheetCompiler.getMakeType(project, runType)

  private val virtualFile = worksheetFile.getVirtualFile

  private def toError(ex: Throwable): RemoteServerConnectorResult.UnhandledError = ex match {
    case ex: ScalaSdkNotConfiguredException => RemoteServerConnectorResult.ExpectedError(ex)
    case ex                                 => RemoteServerConnectorResult.UnexpectedError(ex)
  }
  
  private def isShowErrorsInViewerEditor(autoTriggered: Boolean): Boolean =
    autoTriggered && WorksheetUtils.showReplErrorsInEditorInInteractiveMode || WorksheetUtils.showReplErrorsInEditor

  private def compileAndRun(autoTriggered: Boolean, editor: Editor, request: WorksheetCompileRunRequest, printer: WorksheetEditorPrinter)
                           (originalCallback: EvaluationCallback): Unit = {
    WorksheetCompilerUtil.removeOldMessageContent(project) //or not?

    val logUnexpectedException: Throwable => Unit = ex => {
      val message = s"Unexpected exception occurred during worksheet execution, ${errorDetails(module).mkString(", ")}, ${runType.getName}, $makeType"
      val exc = new RuntimeException(message, ex)
      if (ApplicationManager.getApplication.isUnitTestMode)
        Log.warn(exc) // to not throw TestLoggerAssertionError before printing the error message to the worksheet output
      else
        Log.error(exc) // wrap into an extra exception to conveniently track the logging place
    }
    val progressTitle = WorksheetBundle.message("worksheet.compilation", worksheetFile.getName)
    //on auto-run (interactive mode) do not show error messages in build tool window (via CompilerTask)

    val showErrorsInViewerEditor = isShowErrorsInViewerEditor(autoTriggered)

    val worksheetEvaluation: WorksheetEvaluationBase =
      if (showErrorsInViewerEditor && runType == WorksheetExternalRunType.ReplRunType)
        new WorksheetEvaluationIgnoringErrors(project, progressTitle, logUnexpectedException, printer)
      else
        new WorksheetEvaluationCompilerTaskBased(project, progressTitle, logUnexpectedException, printer)

    // this is needed to close the timer of printer in one place
    val callback: EvaluationCallback = result => {
      if (!worksheetEvaluation.isCompiledWithErrors) {
        printer.flushBuffer()
      }
      printer.close()
      originalCallback(result)
    }

    val cache = WorksheetCache.getInstance(project)
    if (ApplicationManager.getApplication.isUnitTestMode) {
      cache.addCompilerMessagesCollector(editor, worksheetEvaluation)
    }

    val args = request match {
      case RunRepl(code, evaluatedElements) =>
        val replPrinter = printer.asInstanceOf[WorksheetEditorPrinterRepl]
        replPrinter.updateMessagesConsumer(worksheetEvaluation)
        replPrinter.updateEvaluatedElements(evaluatedElements)

        val lastProcessedLine = WorksheetCache.getInstance(project).getLastProcessedIncremental(editor)
        val dropCachedReplInstance = lastProcessedLine.isEmpty
        ReplModeArgs(virtualFile.getCanonicalPath, dropCachedReplInstance, code)
      case RunCompile(code, className) =>
        val (_, tempFile, outputDir) = cache.updateOrCreateCompilationInfo(virtualFile.getCanonicalPath, worksheetFile.getName)
        Files.writeString(tempFile, code)
        PlainModeArgs(tempFile, outputDir, className)
    }
    val afterCompileCallback: RemoteServerConnectorResult => Unit = {
      case RemoteServerConnectorResult.Done =>
        if (worksheetEvaluation.isCompiledWithErrors) {
          callback(WorksheetCompilerResult.CompilationError)
        } else {
          makeType match {
            case OutOfProcess =>
              val plainArgs = args.asInstanceOf[PlainModeArgs]
              WorksheetCompilerLocalEvaluator.executeWorksheet(virtualFile,
                plainArgs.className, plainArgs.outputDir.toCanonicalPath.toString)(callback, printer)(using module)
            case _ =>
              callback(WorksheetCompilerResult.CompiledAndEvaluated)
          }
        }
      case error: RemoteServerConnectorResult.UnhandledError          =>
        callback(WorksheetCompilerResult.RemoteServerConnectorError(error))
    }
    worksheetEvaluation.start(() => {
      try {
        val connector = new RemoteServerConnector(module, worksheetFile, args, makeType)
        connector.compileAndRun(virtualFile, worksheetEvaluation)(afterCompileCallback)
      } catch {
        case NonFatal(ex) =>
          afterCompileCallback(toError(ex))
      }
    })
  }

  def compileAndRun(autoTriggered: Boolean, editor: Editor, printer: WorksheetEditorPrinter)
                   (originalCallback: EvaluationCallback): Unit = try {
    Log.traceWithDebugInDev(s"compileAndRun, autoTriggered: $autoTriggered")

    runType.process(worksheetFile, editor) match {
      case Right(request) =>
        compileAndRun(autoTriggered, editor, request, printer)(originalCallback)
      case Left(preprocessError)  =>
        originalCallback(WorksheetCompilerResult.PreprocessError(preprocessError))
    }
  } catch {
    case NonFatal(ex) =>
      originalCallback(WorksheetCompilerResult.UnknownError(ex))
  }
  
  @RequiresEdt
  def createWorksheetEditorPrinter(editor: Editor, autoTriggered: Boolean): WorksheetEditorPrinter = {
    val showErrorsInViewerEditor = isShowErrorsInViewerEditor(autoTriggered)
    runType match {
      case WorksheetExternalRunType.PlainRunType =>
        WorksheetEditorPrinterFactory.getDefaultUiFor(editor, worksheetFile)
      case WorksheetExternalRunType.ReplRunType =>
        WorksheetEditorPrinterFactory.getIncrementalUiFor(editor, worksheetFile, showReplErrorsInEditor = showErrorsInViewerEditor)
    }
  }
}

//private[worksheet]
object WorksheetCompiler {

  private val Log: Logger = Logger.getInstance(getClass)

  type EvaluationCallback = WorksheetCompilerResult => Unit

  sealed trait WorksheetCompilerResult
  object WorksheetCompilerResult {
    // worksheet was compiled without any errors (but warnings possible) and evaluated successfully
    object CompiledAndEvaluated extends WorksheetCompilerResult
    object Compiled extends WorksheetCompilerResult

    sealed trait WorksheetCompilerError extends WorksheetCompilerResult
    final case class PreprocessError(error: WorksheetPreprocessError) extends WorksheetCompilerError
    case object CompilationError extends WorksheetCompilerError
    final case class ProcessTerminatedError(returnCode: Int, message: String) extends WorksheetCompilerError
    final case class ProjectIsAlreadyDisposed(name: String, trace: Throwable = new Throwable) extends WorksheetCompilerError
    final case class RemoteServerConnectorError(error: RemoteServerConnectorResult.UnhandledError) extends WorksheetCompilerError
    final case class UnknownError(cause: Throwable) extends WorksheetCompilerError // TODO: maybe wrap result into Try instead?
  }

  trait CompilerMessagesCollector {
    def collectedMessages: Seq[CompilerMessage]
  }

  private abstract class WorksheetEvaluationBase(
    logUnexpectedException: Throwable => Unit,
    worksheetPrinter: WorksheetEditorPrinter
  ) extends WorksheetEvaluation
    with CompilerMessagesConsumer
    with CompilerMessagesCollector {

    protected val messages: mutable.ArrayBuffer[CompilerMessage] = mutable.ArrayBuffer()

    protected var hasCompilationErrors = false

    override def isCompiledWithErrors: Boolean = hasCompilationErrors

    override def collectedMessages: Seq[CompilerMessage] = messages.toSeq

    protected def progressIndicator: ProgressIndicator

    override def progress(text: String, done: Option[Float]): Unit = {
      val taskIndicator = progressIndicator
      if (taskIndicator != null) {
        taskIndicator.setText(text)
        done.foreach(d => taskIndicator.setFraction(d.toDouble))
      }
    }

    override def worksheetOutput(text: String): Unit = {
      worksheetPrinter.processLine(text)
    }

    override def trace(ex: Throwable): Unit = {
      logUnexpectedException(ex)
      worksheetPrinter.internalError(ex)
    }
  }

  /**
   * upstream: runs in a CompilerTask, which shows the compiler messages in the build tool window (CompilerTask is
   * internal in Consulo), here the messages are shown in the Messages tool window after the evaluation
   */
  private class WorksheetEvaluationCompilerTaskBased(
    project: Project,
    @Nls progressTitle: String,
    logUnexpectedException: Throwable => Unit,
    worksheetPrinter: WorksheetEditorPrinter,
  ) extends WorksheetEvaluationBase(logUnexpectedException, worksheetPrinter) {

    @volatile private var _progressIndicator: ProgressIndicator = scala.compiletime.uninitialized
    override protected def progressIndicator: ProgressIndicator = _progressIndicator

    private val reportedMessages = mutable.ArrayBuffer.empty[CompilerMessage]

    private val isUnitTestMode = ApplicationManager.getApplication.isUnitTestMode

    override def message(message: CompilerMessage): Unit = synchronized {
      if (message.getCategory == CompilerMessageCategory.ERROR) {
        hasCompilationErrors = true
      }

      if (message.getCategory == CompilerMessageCategory.ERROR || message.getCategory == CompilerMessageCategory.WARNING) {
        reportedMessages += message
      }
      // for now we only need the compiler messages in unit tests
      if (isUnitTestMode) {
        messages += message
      }
    }

    override def start(runnable: Runnable): Unit = {
      val backgroundTask = new Task.Backgroundable(project, progressTitle, false) {
        override def run(indicator: ProgressIndicator): Unit = {
          _progressIndicator = indicator
          try {
            runnable.run()
          } finally {
            WorksheetCompilerUtil.showCompilerMessages(project, synchronized(reportedMessages.toSeq))
          }
        }
      }
      ProgressManager.getInstance.run(backgroundTask)
    }
  }

  private class WorksheetEvaluationIgnoringErrors(
    project: Project,
    @Nls progressTitle: String,
    logUnexpectedException: Throwable => Unit,
    worksheetPrinter: WorksheetEditorPrinter,
  ) extends WorksheetEvaluationBase(logUnexpectedException, worksheetPrinter) {

    private var _progressIndicator: ProgressIndicator = scala.compiletime.uninitialized
    override protected def progressIndicator: ProgressIndicator = _progressIndicator

    override def message(message: CompilerMessage): Unit = {
      //do nothing
    }

    override def start(runnable: Runnable): Unit = {
      val backgroundTask = new Task.Backgroundable(project, progressTitle, false) {
        override def run(indicator: ProgressIndicator): Unit = {
          _progressIndicator = indicator
          runnable.run()
        }
      }
      ProgressManager.getInstance.run(backgroundTask)
    }
  }

  private def errorDetails(module: Module): Seq[String] =
    if (module.isDisposed) Seq() else {
      val scalaMajorVersion = module.scalaLanguageLevel.map("Scala: " + _)
      val project = module.getProject
      // upstream: the JDK of the compile server
      val jdk: Option[String] =
        if (project.isDisposed) None else {
          val sdk = Option(ModuleUtilCore.getSdk(module, classOf[JavaModuleExtension[?]]))
          val version = sdk.flatMap(sdk => Option(sdk.getVersionString)).flatMap(JDKVersionDetector.jdkVersion)
          version.map("JDK: " + _)
        }

      scalaMajorVersion.toSeq ++ jdk
    }

  private object JDKVersionDetector {
    // non-existing versions added just in case for the future...
    private val versions = Seq(16, 15, 14, 13, 12, 11, 9, 8)
    // assuming that jdk name is a sensitive information, so can't directly use jdkName
    def jdkVersion(jdkVersionString: String): Option[Int] =
      versions.find(v => jdkVersionString.contains(v.toString))
  }

  // upstream: OutOfProcessServer only with the compile server enabled and "Run worksheet in the compiler process" off,
  // NonServer without compile server
  private def getMakeType(project: Project, runType: WorksheetExternalRunType): WorksheetMakeType =
    if (ScalaProjectSettings.getInstance(project).isInProcessMode || runType == WorksheetExternalRunType.ReplRunType) {
      InProcess
    } else {
      OutOfProcess
    }
}
