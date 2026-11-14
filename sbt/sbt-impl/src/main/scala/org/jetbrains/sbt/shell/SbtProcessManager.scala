package org.jetbrains.sbt.shell

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.{Inject, Singleton}
import consulo.execution.configuration.EnvironmentVariablesData
import consulo.process.cmd.GeneralCommandLine
import com.intellij.java.execution.configurations.RemoteConnection
import consulo.process.cmd.ParametersList
import consulo.process.cmd.GeneralCommandLine.ParentEnvironmentType
import com.pty4j.{PtyProcess, WinSize}
import consulo.process.{NativeProcessHandler, ProcessConsoleType, ProcessHandlerBuilder, ProcessHandlerFeature}
import consulo.project.ui.notification.NotificationType
import consulo.disposer.Disposable
import consulo.logging.Logger
import consulo.document.FileDocumentManager
import consulo.application.progress.ProgressManager
import consulo.application.ApplicationManager
import consulo.project.Project
import consulo.project.event.ProjectManagerListener
import consulo.disposer.Disposer
import consulo.application.platform.PlatformTransfer
import consulo.platform.Platform
import consulo.process.local.EnvironmentUtil
import consulo.ui.annotation.RequiredUIAccess
import consulo.component.messagebus.MessageBusConnection
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.api.isUnitTestMode
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups
import org.jetbrains.sbt.buildinfo.BuildInfo
import org.jetbrains.sbt.process.mock.MockSbtProcessForTests
import org.jetbrains.sbt.process.options.reporting.WarningsCollectingBuildReporter
import org.jetbrains.sbt.process.options.{SbtProcessOptions, SbtProcessOptionsResolver}
import org.jetbrains.sbt.process.{SbtProcessEnvironment, SbtProcessOutputDiagnosticsCollector, SbtRunner}
import org.jetbrains.sbt.project.SbtExternalSystemManager
import org.jetbrains.sbt.project.settings.SbtExecutionSettings
import org.jetbrains.sbt.shell.SbtProcessManager.*
import org.jetbrains.sbt.shell.communication.SbtShellLifecycle.ShellStateEvent
import org.jetbrains.sbt.shell.optionsWarn.SbtShellOptionsWarningService
import org.jetbrains.sbt.shell.process.utils.{SbtSettingsInjector, SbtShellJdkSelector, SbtShellRunId, SbtShellVmOptionsBuilder}
import org.jetbrains.sbt.{SbtBundle, SbtUtil, SbtVersionDetector, normalizedLocalPath}
import org.jetbrains.sbt.api.{SbtVersion, SbtVersionCapabilities}

import java.io.{OutputStreamWriter, PrintWriter}
import java.nio.file.Path
import java.util.concurrent.{ConcurrentHashMap, TimeUnit, TimeoutException}
import scala.jdk.CollectionConverters.*

/**
 * Manages the sbt shell process instance for the project.
 * Instantiates an sbt instance when initially requested.
 */
//noinspection ApiStatus,UnstableApiUsage
// upstream: @Service(Array(Service.Level.PROJECT))
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
final class SbtProcessManager @Inject()(project: Project) extends Disposable {

  private val messageBus: MessageBusConnection = ApplicationManager.getApplication.getMessageBus.connect(this)
  messageBus.subscribe(classOf[ProjectManagerListener], new ProjectManagerListener() {
    override def projectClosing(p: Project): Unit = {
      if (project == p)
        SbtProcessManager.instanceIfCreated(project).foreach(Disposer.dispose)
    }
  })

  import SbtProcessManager.ProcessData

  private val log = Logger.getInstance(getClass)
  private val shownEnvironmentWarningMessages = ConcurrentHashMap.newKeySet[String]()

  @volatile private var processData: Option[ProcessData] = None
  private var processDestroyInProgress: Option[ProcessData] = None
  private val processDataMutex = new Object
  private val processStartupMutex = new Object

  private case class ProcessDestroyRequest(
    pd: ProcessData,
    shell: SbtShellCommunication,
    shouldEmitProcessTerminated: Boolean,
  )

  /**
   * @return A tuple containing:
   *         - `NativeProcessHandler`: the configured process handler instance for the sbt shell.
   *         - `Option[RemoteConnection]`: debug connection information if the shell debug mode is enabled; otherwise, None.
   *         - `SbtVersion`: detected sbt version used in the project.
   *         - `Boolean`: A flag indicating whether the "new sbt shell" is being used.
   */
  private def createShellProcessHandler: (NativeProcessHandler, Option[RemoteConnection], SbtVersion, Boolean) = {
    log.debug("createShellProcessHandler")
    val workingDirPath = SbtUtil.getWorkingDirPath(project)
    val workingDir = Path.of(workingDirPath)

    val sbtSettings = getSbtSettings(workingDirPath)
    lazy val launcher = PlatformTransfer.getInstance().transfer(SbtUtil.getLauncherJar(sbtSettings), project.getPlatform)

    // Use sbtStructureVersion as an approximation of compatible IDEA versions.
    val runId: SbtShellRunId =
      SbtShellRunId(BuildInfo.sbtStructureVersion)

    saveAllDocumentsToDisk()

    val projectSbtVersion = SbtUtil.detectSbtVersion(workingDir, launcher)
    val addPluginCommandSupported = SbtVersionCapabilities.isAddPluginCommandSupported(projectSbtVersion)
    log.debug(s"projectSbtVersion = $projectSbtVersion")
    log.debug(s"addPluginCommandSupported = $addPluginCommandSupported")

    // Allow using the "new shell" only with sbt >= 1.4, because starting from this version, JLine 3 is utilized within the "shell" command.
    // This provides better completion support and is used for detecting when the shell is ready
    // (see org.jetbrains.sbt.shell.communication.SbtShellOutputRecognizer.isPromptReady).
    // upstream: Registry.is("sbt.new.shell") && projectSbtVersion >= 1.4 - the new shell runs in a TerminalExecutionConsole,
    // Consulo has no terminal execution console, so only the old shell (language console) is used
    val useNewShell = false

    val optionsReporter = new WarningsCollectingBuildReporter
    val shellSbtProcessOptions: SbtProcessOptions =
      SbtProcessOptionsResolver.resolveSbtOptionsForShell(
        workingDir,
        sbtSettings.sbtOptions.options,
        EnvironmentVariablesData.create(sbtSettings.userSetEnvironment.asJava, sbtSettings.passParentEnvironment),
        malformedSbtOptionsFromSettings = sbtSettings.sbtOptions.malformedOptions
      )(using optionsReporter)
    SbtShellOptionsWarningService.instance(project).showWarnings(optionsReporter.collectedWarnings)

    val vmOptionsData: SbtShellVmOptionsData =
      new SbtShellVmOptionsBuilder().createVmOptions(
        sbtSettings,
        workingDir,
        addPluginCommandSupported,
        runId,
        shellSbtProcessOptions.allVmOptions,
        useNewShell = useNewShell
      )

    val vmOptions: ParametersList =
      vmOptionsData.vmOptions

    val settingsInjector = new SbtSettingsInjector(project)
    val settingsFile: Path =
      settingsInjector.getOrCreateSettingsFileWithInjectedSettings(runId, projectSbtVersion, addPluginCommandSupported, vmOptions)

    val programParams: ParametersList =
      createProgramParameters(
        settingsFile,
        shellSbtProcessOptions.sbtLauncherArgs,
        addPluginCommandSupported = addPluginCommandSupported,
        useNewShell = useNewShell
      )

    val vmExecutable: Path =
      new SbtShellJdkSelector(project).selectVmExecutableForSettings(sbtSettings)

    val commandLine: GeneralCommandLine =
      createPtyCommandLine(vmExecutable, workingDir, vmOptions, launcher, programParams, sbtSettings.passParentEnvironment, sbtSettings.userSetEnvironment, useNewShell)

    // upstream: PtyCommandLine with initial PTY window size and console mode settings - the console mode can't be set in Consulo,
    // the window size is set right after the start (see patchInitialPtyWindowSize)
    val processHandler: NativeProcessHandler =
      ProcessHandlerBuilder.create(commandLine)
        .killable()
        .colored()
        .consoleType(ProcessConsoleType.EXTERNAL_EMULATION)
        .shouldKillProcessSoftly(true)
        .build()
        .asInstanceOf[NativeProcessHandler]
    patchInitialPtyWindowSize(processHandler, useNewShell)

    SbtProcessOutputDiagnosticsCollector.collectProcessOutputFrom(
      processHandler,
      processTitle = s"SBT shell process output (${if (useNewShell) "new-shell" else "old-shell"})",
    )

    (processHandler, vmOptionsData.debugConnection, projectSbtVersion, useNewShell)
  }

  /**
   * By saving all documents we ensure that edits in `project/build.properties` are saved to disk.<br>
   * Otherwise, user might change `sbt.version`, reload the project, and there will be a warning in sbt shell:
   * ```
   * [warn] sbt version mismatch, using: 1.9.1, in build.properties: "1.9.2", use 'reboot' to use the new value.
   * ```
   *
   * Without this call the `saveAllDocuments` would be called anyway after the sbt process is started.
   * But it would be too late already, because the warning would be already generated by sbt.
   */
  private def saveAllDocumentsToDisk(): Unit = {
    if (!isUnitTestMode) {
      invokeAndWait {
        inWriteAction {
          FileDocumentManager.getInstance().saveAllDocuments()
        }
      }
    }
  }

  private def createProgramParameters(
    settingsFile: Path,
    sbtLauncherArgs: Seq[String],
    addPluginCommandSupported: Boolean,
    useNewShell: Boolean,
  ): ParametersList = {
    val programParams = new ParametersList

    if (addPluginCommandSupported) {
      val settingsPath = settingsFile.normalizedLocalPath
      programParams.add(s"early(addPluginSbtFile=\"\"\"$settingsPath\"\"\")")
    }

    val commands = if (useNewShell) "shell" else "idea-shell"

    programParams.add(commands)
    programParams.addAll(sbtLauncherArgs *)

    programParams
  }

  private def getSbtSettings(dir: String): SbtExecutionSettings =
    SbtExternalSystemManager.executionSettingsFor(project, dir)

  /**
   * Because the regular GeneralCommandLine process doesn't mesh well with JLine on Windows, use a
   * Pseudo-Terminal based command line.
   */
  private def createPtyCommandLine(
    vmExecutable: Path,
    workingDir: Path,
    vmParams: ParametersList,
    launcher: Path,
    programParams: ParametersList,
    passParentEnvironment: Boolean,
    environment: Map[String, String],
    withNewShell: Boolean
  ): GeneralCommandLine = {
    val preparedEnvironment = SbtProcessEnvironment.prepare(
      environment,
      passParentEnvironment,
      SbtProcessEnvironment.isTerminalPropsSanitizationEnabled
    )
    preparedEnvironment.warnings.foreach { warning =>
      log.warn(s"${warning.message}\n${warning.details}")
      if (shownEnvironmentWarningMessages.add(warning.message))
        ScalaNotificationGroups.sbtShell.createNotification(warning.message, warning.details.replace("\n", "<br>"), NotificationType.WARNING, null).notify(project)
    }

    val pty = new GeneralCommandLine()
    pty.withExePath(vmExecutable.toString)
    pty.withWorkingDirectory(workingDir)
    pty.withEnvironment(preparedEnvironment.variables.asJava)

    if (isUnitTestMode && Platform.current.os.isWindows)
      pty.withEnvironment(SbtRunner.defaultCoursierDirectoriesAsEnvVariables().asJava)

    pty.addParameters(vmParams.getList)

    if (MockSbtProcessForTests.isEnabled(project)) {
      pty.addParameters(MockSbtProcessForTests.mockMainClassCommandLineTailForSbtShell(project, withNewShell) *)
    } else {
      pty.addParameters("-jar", launcher.normalizedLocalPath)
      pty.addParameters(programParams.getList)
    }

    if (shouldUnsetCIEnvVars(passParentEnvironment)) {
      unsetCIEnvVars(pty)
    } else {
      val parentEnvironmentType = if (passParentEnvironment) ParentEnvironmentType.CONSOLE else ParentEnvironmentType.NONE
      pty.withParentEnvironmentType(parentEnvironmentType)
    }

    pty
  }

  private def patchInitialPtyWindowSize(processHandler: NativeProcessHandler, withNewShell: Boolean): Unit = {
    /*
     upstream: PtyCommandLine.withInitialColumns/withInitialRows - Consulo's pty command line has no initial size options,
     the size of the started PTY process is set instead

     Setting an initial PTY window size for the sbt shell pty process.

     Initially (in the "old shell"), this was only required on Windows since the terminal defaults to 80 columns,
     causing line wrapping and breaking highlighting. However, with the new shell enabled,
     it is required for both Windows and Unix (due to the use of jline3 in the built-in "shell" command since sbt 1.4).
     This is because if the shell window is minimized at startup, the terminal may report a 0×0 size.
     As a result, ">..." (see `org.jline.reader.impl.LineReaderImpl#redisplay`) will be displayed instead of the shell prompt,
     which breaks the shell ready mechanism.

     Behavior:
       - New shell: applies on both Unix and Windows.
       - Old shell: applies on Windows only.

     Dynamic window size adjustments can be done in the custom TTY connector (see [[createTerminalConsole]]).
     */
    val shouldPatchInitialSize = withNewShell || Platform.current.os.isWindows
    processHandler.getProcess match {
      case ptyProcess: PtyProcess if shouldPatchInitialSize =>
        try ptyProcess.setWinSize(new WinSize(2000, 100))
        catch {
          case e: Exception => log.warn("Can't set the initial PTY window size of the sbt shell", e)
        }
      case _ =>
    }
  }

  /**
   * Whether the `BUILD_NUMBER` and `CI` environment variables should be removed from the sbt shell process.
   *
   * When the sbt shell runs on CI, it can inherit these variables from the parent environment. I only observed `BUILD_NUMBER` to be set,
   * but it's enough for sbt to treat the environment as CI and change its behavior.
   * https://github.com/sbt/sbt/blob/ae1192109d5b0ffd206f6f079c6b14ce25b995d4/internal/util-logging/src/main/scala/sbt/internal/util/Terminal.scala#L233
   *
   * The most visible consequence is that sbt disables its terminal management (https://github.com/sbt/sbt/blob/ae1192109d5b0ffd206f6f079c6b14ce25b995d4/internal/util-logging/src/main/scala/sbt/internal/util/Terminal.scala#L400)
   * and therefore does not switch the PTY into raw input mode, so the PTY stays in canonical mode (input is read line by line). A single-character input
   * written to the shell - such as the `i` (ignore) is then never read, because it's sent without a trailing newline.
   * Unsetting these variables in the sbt shell process when running tests on CI makes sbt behave the same way as it does locally.
   *
   * Note: this most likely affects only prompts shown while sbt is starting. When a single-character input is sent while an sbt command is running,
   * sbt appears to switch to raw mode, which behaves the same way as it does locally.
   *
   * This is only done in tests running on TeamCity, and only when the parent environment is actually inherited
   * (`passParentEnvironment`); otherwise there is nothing to unset.
   */
  private def shouldUnsetCIEnvVars(passParentEnvironment: Boolean): Boolean = {
    val isRunningInTeamcity = sys.env.contains("TEAMCITY_VERSION")
    passParentEnvironment && isUnitTestMode && isRunningInTeamcity
  }

  /**
   * Removes the `BUILD_NUMBER` and `CI` environment variables from the sbt shell process environment.
   *
   * To remove these variables, we get the full environment map used by the `CONSOLE` type (the same way
   * `GeneralCommandLine.getParentEnvironment` does), drop the unwanted keys, and set the
   * resulting environment map. The parent environment type must then be set explicitly to [[ParentEnvironmentType.NONE]],
   * so the parent environment is not inherited again, which would otherwise re-add the removed keys.
   *
   * @see [[shouldUnsetCIEnvVars]]
   */
  private def unsetCIEnvVars(pty: GeneralCommandLine): Unit = {
    val envVarsToUnset = Set("BUILD_NUMBER", "CI")
    // Explicit vars take precedence over the parent ones, matching the merge order in `GeneralCommandLine.setupEnvironment`.
    val curatedEnvironment = (EnvironmentUtil.getEnvironmentMap.asScala.toMap ++ pty.getEnvironment.asScala) -- envVarsToUnset
    pty.getEnvironment.clear()
    pty.withEnvironment(curatedEnvironment.asJava)
    pty.withParentEnvironmentType(ParentEnvironmentType.NONE)
  }

  /**
   * Send a single CTRL+C/SIGINT signal
   * If a task is already running, it terminates the currently running task.
   * If no task is running, this request might result in terminating the sbt shell.
   */
  def requestTaskCancellation(): Unit = {
    processData match {
      case Some(pd) =>
        log.debug("requestTaskCancellation start: terminateProcessGracefully...")

        sendSigIntOrDestroy(pd.processHandler)

        log.debug("requestTaskCancellation finish")
      case _ =>
        log.debug("requestTaskCancellation: skipping (no process data available)")
    }
  }

  /** Asynchronously initializes SbtShellRunner with sbt process, console UI, and optionally opens sbt shell window. */
  def initAndRunAsync(activateSbtShellToolWindowOnStartup: Boolean = currentProcessToolWindowActivationOnStartup): Unit = {
    log.debug("initAndRunAsync start...")

    executeOnPooledThread {
      acquireShellProcessHandler(activateSbtShellToolWindowOnStartup)
      if (activateSbtShellToolWindowOnStartup) {
        SbtShellRunner.openShell(focus = true, project)
      }

      log.debug("initAndRunAsync finish")
    }
  }

  private def currentProcessToolWindowActivationOnStartup: Boolean =
    processDataMutex.synchronized {
      processData match {
        case Some(pd) if isAlive(pd) =>
          pd.activateSbtShellToolWindowOnStartup
        case _ => true
      }
    }

  private def updateProcessData(activateSbtShellToolWindowOnStartup: Boolean): ProcessData = {
    log.debug("updateProcessData start...")

    val _processData = createProcessData(activateSbtShellToolWindowOnStartup)
    processDataMutex.synchronized {
      processData = Some(_processData)

      val communication = SbtShellCommunication.forProject(project)
      communication.initCommunication(_processData.processHandler)
    }

    _processData match {
      case pd: AbstractConsoleProcessData =>
        pd.runner.initAndRun()
    }

    log.debug(s"updateProcessData finish (sbtVersion=${_processData.sbtVersion}, isNewShell=${_processData.isNewShell})")

    _processData
  }

  private def createProcessData(activateSbtShellToolWindowOnStartup: Boolean): ProcessData = {
    val (handler, debugConnection, sbtVersion, useNewShell) = createShellProcessHandler

    val title = project.getName
    val runner = new SbtShellRunner(project, title, debugConnection, activateSbtShellToolWindowOnStartup)
    AbstractConsoleProcessData(handler, sbtVersion, debugConnection, runner, useNewShell, activateSbtShellToolWindowOnStartup)
  }

  /** Supply a PrintWriter that writes to the current process. */
  def usingWriter[T](f: PrintWriter => T): T = {
    val processInput = acquireShellProcessHandler().getProcessInput
    val writer = new PrintWriter(new OutputStreamWriter(processInput))
    f(writer)
  }

  /**
   * Request an sbt she'll process instance. It will be started if necessary.
   * The process handler should only be used to access the running process!
   * SbtProcessManager is solely responsible for handling the running state.
   *
   * The background thread is required because other parts of the invoked code also require it
   * (for example [[SbtExternalSystemManager.executionSettingsFor]]).
   */
  // upstream: @RequiresBackgroundThread
  def acquireShellProcessHandler(activateSbtShellToolWindowOnStartup: Boolean = true): NativeProcessHandler = {
    log.debug("acquireShellProcessHandler start...")
    if (isUnitTestMode)
      assert(!ApplicationManager.getApplication.isDispatchThread, "must be called on a background thread")

    existingAliveProcessData match {
      case Some(pd) if isAlive(pd) =>
        log.debug("acquireShellProcessHandler finish: reusing existing alive process handler")
        pd.processHandler
      case _ =>
        processStartupMutex.synchronized {
          waitForProcessDestroyToFinish()
          existingAliveProcessData match {
            case Some(pd) if isAlive(pd) =>
              log.debug("acquireShellProcessHandler finish: reusing existing alive process handler")
              pd.processHandler
            case _ =>
              log.debug("acquireShellProcessHandler: no alive process, creating new one...")
              val handler = updateProcessData(activateSbtShellToolWindowOnStartup).processHandler
              log.debug("acquireShellProcessHandler finish: created new process handler")
              handler
          }
        }
    }
  }

  private def existingAliveProcessData: Option[ProcessData] =
    processDataMutex.synchronized {
      if (processDestroyInProgress.nonEmpty) None
      else processData.filter(isAlive)
    }

  @TestOnly
  // upstream: @Internal
  def flushConsoleOutputForTests(): Unit =
    processData match {
      case Some(pd) if isAlive(pd) =>
        invokeAndWait {
          pd.flushText()
        }
      case _ =>
        throw new Exception("Process data is not available")
    }

  /**
   * Checks whether the sbt version used by the sbt shell process matches the current sbt version.
   * Returns `false` if the sbt shell is not running.
   *
   * @see [[org.jetbrains.sbt.SbtVersionDetector.detectSbtVersion]]
   */
  def isSbtVersionOutdated: Boolean = processDataMutex.synchronized {
    processData.filter(isAlive).exists { data =>
      data.sbtVersion != SbtVersionDetector.detectSbtVersion(project)
    }
  }

  /**
   * @return Some shell runner if sbt shell is already running<br>
   *         None if sbt shell is not running
   */
  def shellRunner: Option[SbtShellRunner] = processData.collect { case x: AbstractConsoleProcessData => x.runner }
  def debugConnection: Option[RemoteConnection] = processData.flatMap(_.debugConnection)

  // upstream: @RequiresBackgroundThread
  def restartProcess(): Unit = {
    doRestartProcess()
  }

  private def doRestartProcess(): Unit = processStartupMutex.synchronized {
    log.debug("restartProcess")
    destroyProcess()
    waitForProcessDestroyToFinish()
    updateProcessData(activateSbtShellToolWindowOnStartup = true)
  }

  //TODO: extract common "retry" utilities
  private def terminateProcessGracefully(processHandler: NativeProcessHandler): Unit = {
    val process = processHandler.getProcess
    def attemptTermination(): Unit =
      sendSigIntOrDestroy(processHandler)

    // 1 try and 4 retries, will wait 3 seconds, 6 seconds, 9 seconds and 12 seconds between each retry
    // before finally giving up and stopping the process by force
    var tries = 5
    var success = false
    var timeout = 3L
    val backoff = 3L // Back off for additional 3 seconds before each retry.

    while (!success && tries > 0) {
      attemptTermination()
      try {
        process.onExit().get(timeout, TimeUnit.SECONDS)
        success = true
      } catch {
        case _: TimeoutException =>
          timeout += backoff
          tries -= 1
      }
    }

    if (!success) {
      process.destroyForcibly()
    }
  }

  /**
   * Perform a "hard" destroy of the sbt shell, where the emptying queue process is canceled if it is running.
   */
  def destroyProcess(): Unit =
    destroyProcess(isSoft = false)

  /**
   * Perform a "soft" destroy of the sbt shell, where the emptying queue process is not canceled, and post-restart commands are preserved.
   */
  def softDestroyProcess(): Unit =
    destroyProcess(isSoft = true)

  /**
   * @param isSoft when `true`, the after-restart commands buffer is preserved.
   *               When `false`, the soft-restart is canceled if it is running.
   */
  private def destroyProcess(isSoft: Boolean): Unit =
    prepareDestroyProcess(isSoft).foreach { request =>
      try {
        runProcessTermination(request.pd)
        finishDestroyProcess(request)
      } catch {
        case ex: Throwable =>
          clearProcessDestroyInProgress(request.pd)
          throw ex
      }
    }

  private def prepareDestroyProcess(isSoft: Boolean): Option[ProcessDestroyRequest] = processDataMutex.synchronized {
    log.debug("destroyProcess start...")

    if (processDestroyInProgress.isDefined) {
      // Destruction is already running (possibly on another thread).
      // Do not block waiting for it to finish (that would refreeze the EDT and reintroduce the SCL-25654 deadlock).
      // But a hard kill must still take effect:
      // cancel the commands buffered for an in-progress soft restart so the shell does not come back after the current shutdown completes.
      if (isSoft) {
        log.debug("destroyProcess finish: destroy is already in progress")
      } else {
        log.debug("destroyProcess: hard kill while a shutdown is in progress, cancelling after-restart commands")
        SbtShellCommunication.forProject(project).escalateShutdownToHardKill()
      }
      return None
    }

    processData match {
      case Some(pd) =>
        processDestroyInProgress = Some(pd)

        prepareDestroyProcessInner(isSoft, pd)
      case None => // nothing to do
        log.debug("destroyProcess finish: no processData")
        None
    }

  }

  private def prepareDestroyProcessInner(isSoft: Boolean, pd: ProcessData): Option[ProcessDestroyRequest] = {
    try {
      val shell = SbtShellCommunication.forProject(project)
      val shellStateBeforeDestroy = shell.currentState
      // Startup may fail before the communication layer observes a ready prompt.
      // In that case the process still needs disposal, but the shell lifecycle is already Off.
      val shouldEmitShutdownRequested = !shellStateBeforeDestroy.isShuttingDownOrOff
      // If shutdown was already requested elsewhere, only the terminal event is still needed.
      val shouldEmitProcessTerminated = !shellStateBeforeDestroy.isOff

      // For hard kill: terminate after-restart commands buffer and transition to ShuttingDown
      // For soft restart: transition to ShuttingDown
      if (shouldEmitShutdownRequested) {
        if (!isSoft)
          shell.initiateHardKill()
        else
          shell.enterShuttingDownState()
      }

      Some(ProcessDestroyRequest(pd, shell, shouldEmitProcessTerminated))
    } catch {
      case ex: Throwable =>
        clearProcessDestroyInProgressLocked(pd)
        throw ex
    }
  }

  private def runProcessTermination(pd: ProcessData): Unit = {
    val runnable: Runnable = () => terminateProcessGracefully(pd.processHandler)
    if (isUnitTestMode)
      runnable.run()
    else
      ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, SbtBundle.message("sbt.shell.stopping.process"), false, project)
  }

  private def finishDestroyProcess(request: ProcessDestroyRequest): Unit = processDataMutex.synchronized {
    try {
      if (request.shouldEmitProcessTerminated) {
        log.trace("destroyProcess: emit ProcessTerminated...")
        request.shell.emitShellStateEvent(ShellStateEvent.ProcessTerminated)
      }

      processData match {
        case Some(currentPd) if currentPd eq request.pd =>
          processData = None
          log.debug("destroyProcess finish: processData cleared")
        case Some(_) =>
          log.debug("destroyProcess finish: processData already replaced")
        case None =>
          log.debug("destroyProcess finish: processData already cleared")
      }
    } finally {
      clearProcessDestroyInProgressLocked(request.pd)
    }
  }

  private def waitForProcessDestroyToFinish(): Unit = processDataMutex.synchronized {
    if (processDestroyInProgress.nonEmpty)
      log.debug("waiting for destroyProcess to finish...")
    while (processDestroyInProgress.nonEmpty) {
      processDataMutex.wait()
    }
  }

  private def clearProcessDestroyInProgress(pd: ProcessData): Unit = processDataMutex.synchronized {
    clearProcessDestroyInProgressLocked(pd)
  }

  private def clearProcessDestroyInProgressLocked(pd: ProcessData): Unit = {
    processDestroyInProgress match {
      case Some(currentPd) if currentPd eq pd =>
        processDestroyInProgress = None
        processDataMutex.notifyAll()
      case _ =>
    }
  }

  def sendSigInt(): Unit = processData.foreach(_.processHandler.destroyProcess())

  override def dispose(): Unit = {
    // Intentionally do NOT waitForProcessDestroyToFinish() here: if a destroy is already in progress,
    // destroyProcess() returns immediately and we let it complete on its owning thread.
    // dispose() runs on the EDT during project close, and the in-progress termination needs the EDT to
    // pump its modal progress, so waiting here would re-freeze the EDT and reintroduce the deadlock
    // this class is designed to avoid (see prepareDestroyProcess / runProcessTermination, SCL-25654).
    destroyProcess()
    ConsoleViewsRegistry.disposeLast(project)
  }

  /** Report if shell process is alive. Should only be used for UI/informational purposes. */
  private[shell] def isAlive: Boolean =
    processDataMutex.synchronized {
      processDestroyInProgress.isEmpty && processData.exists(isAlive)
    }

  private def isAlive(processData: ProcessData): Boolean = {
    // processData.processHandler.getProcess.isAlive // TODO: I am not sure which is the best
    !processData.processHandler.isProcessTerminated
  }

  def sbtVersionUsedDuringProcessStart: Option[SbtVersion] =
    processData.map(_.sbtVersion)

  // upstream: OSProcessUtil.terminateProcessGracefully (sends Ctrl+C on Windows) - the shell runs under a PTY,
  // Ctrl+C is typed into it when there are no POSIX signals
  private def sendSigIntOrDestroy(processHandler: NativeProcessHandler): Unit =
    Option(processHandler.getFeature(classOf[ProcessHandlerFeature.POSIX])) match {
      case Some(posix) => posix.sendSignal(ProcessHandlerFeature.POSIX.SIGINT)
      case None =>
        processHandler.getProcess match {
          case ptyProcess: PtyProcess if ptyProcess.isAlive && sendCtrlC(ptyProcess) =>
          case process => process.destroy()
        }
    }

  private def sendCtrlC(process: PtyProcess): Boolean =
    try {
      val input = process.getOutputStream
      input.write(3)
      input.flush()
      true
    } catch {
      case e: java.io.IOException =>
        log.debug("Can't send Ctrl+C to the sbt shell", e)
        false
    }

  private[shell] def isRunWithNewShell: Boolean =
    processData.exists(_.isNewShell)
}

object SbtProcessManager {

  case class SbtShellVmOptionsData(
    vmOptions: ParametersList,
    debugConnection: Option[RemoteConnection],
  )

  def forProject(project: Project): SbtProcessManager = {
    val pm = project.getInstance(classOf[SbtProcessManager])
    if (pm == null) throw new IllegalStateException(s"unable to get component SbtProcessManager for project $project")
    else pm
  }

  private[sbt] def instanceIfCreated(project: Project): Option[SbtProcessManager] = {
    Option(project.getInstanceIfCreated(classOf[SbtProcessManager]))
  }

  private sealed trait ProcessData {
    def processHandler: NativeProcessHandler
    /**
     * Version of sbt detected when launching the sbt process
     */
    def sbtVersion: SbtVersion
    def debugConnection: Option[RemoteConnection]
    def isNewShell: Boolean

    /**
     * Whether creating this sbt shell process was allowed to open or focus the sbt shell tool window.
     *
     * The primary `true` case is the foreground shell lifecycle.
     * Most commonly, the platform has already opened or activated the sbt shell tool window before this code runs,
     * for example because the user opened the tool window or pressed Start/Restart there.
     * The flag means that process startup should not suppress the shell UI's normal follow-up activation/focus behavior.
     *
     * The primary `false` case is run/debug configuration delegation, where sbt shell is started only as a background
     * execution dependency and the Run/Debug tool window should remain the active UI.
     *
     * This is stored with process data because tool window content creation can happen lazily, after the process has
     * already been created by a run configuration. At that later point the original request object is no longer on the
     * stack, so the tool-window factory needs this process-level startup intent to avoid reopening/focusing a shell
     * that was started only as a background run/debug dependency.
     */
    def activateSbtShellToolWindowOnStartup: Boolean

    @RequiredUIAccess
    def flushText(): Unit
  }

  private case class AbstractConsoleProcessData(
    processHandler: NativeProcessHandler,
    sbtVersion: SbtVersion,
    debugConnection: Option[RemoteConnection],
    runner: SbtShellRunner,
    isNewShell: Boolean,
    activateSbtShellToolWindowOnStartup: Boolean,
  ) extends ProcessData {

    @RequiredUIAccess
    override def flushText(): Unit = runner.getConsoleView.flushDeferredText()
  }
}
