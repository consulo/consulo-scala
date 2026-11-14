package org.jetbrains.sbt.shell

import consulo.annotation.component.ExtensionImpl
import consulo.build.ui.FilePosition
import consulo.build.ui.event.EventResult
import consulo.build.ui.progress.{BuildProgress, BuildProgressDescriptor}
import consulo.compiler.{CompileContextEx, CompileDriver, CompilerRunner}
import consulo.component.ProcessCanceledException
import consulo.dataContext.DataContext
import consulo.document.FileDocumentManager
import consulo.externalSystem.importing.ImportSpecBuilder
import consulo.externalSystem.service.project.ExternalSystemProjectRefresher
import consulo.externalSystem.util.ExternalSystemApiUtil as ES
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.module.Module
import consulo.project.Project
import consulo.project.ui.notification.{NotificationAction, NotificationType}
import consulo.virtualFileSystem.util.VirtualFileUtil
import jakarta.inject.Inject
import org.jetbrains.plugins.scala.build.BuildMessages.EventId
import org.jetbrains.plugins.scala.build.{BuildMessages, BuildReporter, CompositeReporter, IndicatorReporter}
import org.jetbrains.plugins.scala.extensions.invokeAndWait
import org.jetbrains.plugins.scala.lang.macros.expansion.ReflectExpansionsCollector
import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.plugins.scala.util.{ExternalSystemVfsUtil, ScalaNotificationGroups}
import org.jetbrains.sbt.api.SbtVersion
import org.jetbrains.sbt.api.icons.Icons
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.settings.SbtSettings
import org.jetbrains.sbt.shell.communication.{SbtShellBuildMessagesEventProcessor, SbtShellCommandRequest}
import org.jetbrains.sbt.{SbtBundle, SbtVersionDetector}

import java.nio.file.Path
import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success}

/**
 * Delegates the "Build" of sbt projects to the sbt shell when "Use sbt shell for builds" is enabled.
 *
 * When [[checkAvailable]] says yes, this runner is used to build the project instead of the built-in Consulo compiler runner
 * (which compiles plain Scala modules with the in-process zinc compiler).
 *
 * upstream: SbtProjectTaskRunnerImpl (IntelliJ ProjectTaskRunner); "Build Artifact" packaging after the sbt build (JPS) is not ported
 */
@ExtensionImpl
final class SbtCompilerRunner @Inject()(project: Project) extends CompilerRunner {

  import SbtCompilerRunner.*

  override def getName: LocalizeValue = LocalizeValue.of(SbtBundle.message("sbt.shell.sbt.build"))

  // In practice "Use sbt shell for build" is a per-linked-external-project setting, not global per-project.
  // Here we assume that if it is enabled for some linked sbt project, then we should use it for all modules (see SCL-24287).
  override def checkAvailable(dataContext: DataContext): CompilerRunner.Result =
    if (isUseSbtShellForBuildEnabled) YesResult
    else CompilerRunner.NO

  private def isUseSbtShellForBuildEnabled: Boolean =
    !project.isDisposed &&
      SbtSettings.getInstance(project).getLinkedProjectsSettings.asScala.exists(_.useSbtShellForBuild)

  override def build(
    compileDriver: CompileDriver,
    context: CompileContextEx,
    buildProgress: BuildProgress[BuildProgressDescriptor],
    isRebuild: Boolean,
    forceCompile: Boolean,
    onlyCheckStatus: Boolean
  ): Boolean = {
    Log.debug(s"build start: project=${project.getName}, isRebuild=$isRebuild, onlyCheckStatus=$onlyCheckStatus")

    if (onlyCheckStatus)
      return false

    val compileScope = context.getCompileScope
    // Filter out modules representing sbt meta-build projects (`-build` modules)
    val modules = compileScope.getAffectedModules.toSeq.filterNot(_.isBuildModule)

    // IMPORTANT NOTE: The "Build" action always runs the build for all individual modules.
    // The "products"/"compile" sbt task from the main/root module would do it only for the root project.
    val sbtVersion: SbtVersion = getOrDetectSbtVersion
    val sbtBuildCommands: Seq[String] =
      SbtBuildCommandsFactory.createBuildCommands(sbtVersion, modules, includeTests = compileScope.includeTestScope)

    if (sbtBuildCommands.isEmpty) {
      // don't run anything if there's no module to run a build for
      if (modules.exists(isSbtModule))
        showNotificationThatExternalSystemHasLostModuleData()

      Log.debug("build finish: no sbt commands")
      return false
    }

    // Ensure all documents are saved to disk before running the sbt compilation
    invokeAndWait {
      FileDocumentManager.getInstance().saveAllDocuments()
    }

    val commandFinal: String =
      if (sbtBuildCommands.size == 1) sbtBuildCommands.head
      else sbtBuildCommands.mkString("all ", " ", "")

    runBuildCommand(commandFinal, context, buildProgress)
    true
  }

  private def isSbtModule(module: Module): Boolean =
    ES.isExternalSystemAwareModule(SbtProjectSystem.Id, module)

  private def runBuildCommand(
    command: String,
    context: CompileContextEx,
    buildProgress: BuildProgress[BuildProgressDescriptor]
  ): Unit = {
    Log.debug(s"runBuildCommand start: command=$command")

    val indicator = context.getProgressIndicator
    val resultPromise: Promise[BuildMessages] = Promise()

    val buildId = BuildMessages.randomEventId
    val report = new CompositeReporter(
      new BuildProgressOutputReporter(buildProgress),
      new IndicatorReporter(indicator)
    )

    val shell = SbtShellCommunication.forProject(project)
    val collector = ReflectExpansionsCollector.getInstance(project)

    report.start()
    collector.compilationStarted()

    // Currently, the entire build output is printed to the build output.
    val resultAggregator = SbtShellBuildMessagesEventProcessor.forBuild(
      project,
      report,
      buildId,
      processOutputCollector = None,
      startMessage = SbtBundle.message("sbt.shell.sbt.build"),
      finishMessage = SbtBundle.message("sbt.shell.sbt.build.finished"),
      onOutputLine = text => collector.processCompilerMessage(text)
    )

    val terminationMessage = "Sbt shell terminated before build command is finished"
    val request = SbtShellCommandRequest(command, resultAggregator, Some(terminationMessage))
      .withQueuedOutputMirroring()
    val requestId = request.requestId

    val commandFuture: Future[BuildMessages] = shell.run(request)

    // block the compile thread until the command is finished or the compilation is canceled
    val buildMessages = CancelableWaitUtil.waitForCancelable(
      commandFuture,
      onCancel = () => shell.removeCommandFromQueueOrCancel(requestId)
    )(resultPromise, indicator)

    buildMessages match {
      case Success(messages) =>
        Log.trace(s"runBuildCommand: result success: status=${messages.status}, errors=${messages.errors.size}, warnings=${messages.warnings.size}")
        reportMessages(context, messages)
        report.finish(messages)
      case Failure(_: ProcessCanceledException) =>
        report.finishCanceled()
      case Failure(error) =>
        Log.trace(s"runBuildCommand: result failure: ${error.getClass.getName}: ${error.getMessage}")
        context.newError(LocalizeValue.of(Option(error.getMessage).getOrElse(SbtBundle.message("sbt.shell.sbt.build.failed")))).add()
        report.finishWithFailure(error)
    }

    // build effects
    try {
      ExternalSystemVfsUtil.refreshRoots(project, SbtProjectSystem.Id, indicator)
    } catch {
      // Suppress the `ProcessCanceledException` that might be thrown by #refreshRoots to ensure the code below runs even if the build is canceled.
      case _: ProcessCanceledException =>
        Log.trace(s"runBuildCommand: refreshRoots canceled (ProcessCanceledException)")
    }

    // upstream: reloads changed classes in the attached debugger session (HotSwapUI.reloadChangedClasses),
    // in Consulo the debugger does it on the compilation finished event
    collector.compilationFinished()

    resultPromise.trySuccess(buildMessages.getOrElse(BuildMessages.empty))

    Log.debug(s"runBuildCommand finish: command=$command")
  }

  private def getOrDetectSbtVersion: SbtVersion =
    SbtProcessManager.instanceIfCreated(project)
      .flatMap(_.sbtVersionUsedDuringProcessStart)
      .getOrElse(SbtVersionDetector.detectSbtVersion(project))

  /**
   * Sometimes the external system loses information about sbt modules.
   * It would be very confusing to users if a build task silently did nothing.
   * We detect such cases and suggest project refresh.
   *
   * @see SCL-15118
   */
  private def showNotificationThatExternalSystemHasLostModuleData(): Unit = {
    val notification = ScalaNotificationGroups.sbtShell.createNotification(
      SbtBundle.message("sbt.shell.sbt.build.failed"),
      SbtBundle.message("sbt.shell.unable.to.build.sbt.project", project.getName),
      NotificationType.ERROR,
      null
    )

    notification.addAction(
      NotificationAction.create(
        LocalizeValue.of(SbtBundle.message("sbt.shell.refresh.sbt.project")),
        (_, _) => ExternalSystemProjectRefresher.getInstance().refreshProjects(new ImportSpecBuilder(project, SbtProjectSystem.Id))
      )
    )

    notification.notify(project)
  }
}

private object SbtCompilerRunner {
  private val Log = Logger.getInstance(classOf[SbtCompilerRunner])

  private val YesResult = new CompilerRunner.YesResult(Icons.SBT)

  /** `/path/to/File.scala:12:5: message` */
  private val MessageWithPosition = """^(.+?):(\d+):(\d+):\s*(.*)$""".r

  /**
   * Reports the errors and warnings collected from the sbt output to the compile context
   * (so they are shown in the compile messages and the build is marked as failed).
   */
  private def reportMessages(context: CompileContextEx, messages: BuildMessages): Unit = {
    messages.errors.foreach { error =>
      addMessage(context.newError, error.getMessage.get)
    }
    messages.warnings.foreach { warning =>
      addMessage(context.newWarning, warning.getMessage)
    }
    if (messages.status == BuildMessages.Error && messages.errors.isEmpty)
      context.newError(LocalizeValue.of(SbtBundle.message("sbt.shell.sbt.build.failed"))).add()
  }

  private def addMessage(newMessage: LocalizeValue => consulo.compiler.CompileContext.MessageBuilder, text: String): Unit =
    text match {
      case MessageWithPosition(path, line, column, message) if isAbsolutePath(path) =>
        newMessage(LocalizeValue.of(message))
          .url(VirtualFileUtil.pathToUrl(path))
          .position(line.toInt, column.toInt)
          .add()
      case _ =>
        newMessage(LocalizeValue.of(text)).add()
    }

  private def isAbsolutePath(path: String): Boolean =
    scala.util.Try(Path.of(path).isAbsolute).getOrElse(false)

  /** Prints the sbt build output to the build progress (Build tool window). */
  private final class BuildProgressOutputReporter(buildProgress: BuildProgress[BuildProgressDescriptor]) extends BuildReporter {
    override def start(): Unit = ()
    override def finish(messages: BuildMessages): Unit = ()
    override def finishWithFailure(err: Throwable): Unit = ()
    override def finishCanceled(): Unit = ()

    override def warning(message: String, position: Option[FilePosition]): Unit = ()
    override def error(message: String, position: Option[FilePosition]): Unit = ()
    override def info(message: String, position: Option[FilePosition]): Unit = ()
    override def clear(file: Path): Unit = ()

    override def log(message: String): Unit =
      buildProgress.output(message + System.lineSeparator(), true)

    override def logErr(message: String): Unit =
      buildProgress.output(message + System.lineSeparator(), false)

    override def startTask(eventId: EventId, parent: Option[EventId], message: String, time: Long): Unit = ()
    override def progressTask(eventId: EventId, total: Long, progress: Long, unit: String, message: String, time: Long): Unit = ()
    override def finishTask(eventId: EventId, message: String, result: EventResult, time: Long): Unit = ()
  }
}
