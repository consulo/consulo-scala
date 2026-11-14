package org.jetbrains.sbt.runner

import com.intellij.java.execution.configurations.JavaCommandLineState
import com.intellij.java.execution.impl.JavaRunConfigurationExtensionManager
import consulo.java.execution.configurations.OwnJavaParameters
import consulo.execution.runner.{ExecutionEnvironment, ProgramRunner}
import com.intellij.java.execution.impl.util.JavaParametersUtil
import com.intellij.java.language.projectRoots.JavaSdkType
import consulo.execution.ExecutionResult
import consulo.execution.executor.Executor
import consulo.execution.ui.console.ConsoleView
import consulo.project.Project
import consulo.content.bundle.{Sdk, SdkTable}
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.rt.util.JarManifestUtils
import org.jetbrains.sbt.SbtUtil
import org.jetbrains.sbt.process.SbtProcessOutputDiagnosticsCollector
import org.jetbrains.sbt.process.mock.MockSbtProcessForTests
import org.jetbrains.sbt.project.settings.SbtExecutionSettings
import org.jetbrains.sbt.project.{<<, SbtExternalSystemManager}
import org.jetbrains.sbt.settings.SbtSettings

import java.nio.file.Path
import java.util

/**
 * Execution state for an [[SbtRunConfiguration]].
 *
 * A state instance is created for every SBT run configuration, regardless of whether the configuration is executed in
 * an existing sbt shell or as a separate JVM process (with a warning, see below)
 *
 * In separate-process mode this class acts as a regular [[JavaCommandLineState]]: the IntelliJ platform calls
 * [[createJavaParameters]], and the returned [[OwnJavaParameters]] describe the sbt launcher JVM process to start.
 * That path configures the working directory, environment, JDK, sbt launcher classpath/main class, VM parameters, and
 * program parameters containing the sbt commands.
 *
 * In sbt-shell mode the custom SBT program runners ([[SbtProgramRunner]] and [[SbtDebugProgramRunner]])
 * intercept this state and submit [[processedCommands]] to the already-running shell instead.
 * In that mode [[createJavaParameters]] is not part of the process launch path;
 * the state is used as a holder for the commands and configuration.
 *
 * @note Background IDE sbt tasks such as project import use [[org.jetbrains.sbt.process.SbtRunner]] instead.
 * That runner owns the process lifecycle directly via `GeneralCommandLine`,
 * while this class adapts run configurations to the IntelliJ run/debug execution infrastructure.
 */
private final class SbtCommandLineState(
  val processedCommands: String,
  val configuration: SbtRunConfiguration,
  environment: ExecutionEnvironment,
) extends JavaCommandLineState(environment) {

  override def execute(executor: Executor, runner: ProgramRunner[?]): ExecutionResult = {
    val result = super.execute(executor, runner)
    Option(result.getProcessHandler).foreach { processHandler =>
      SbtProcessOutputDiagnosticsCollector.collectProcessOutputFrom(
        processHandler,
        processTitle = s"SBT run configuration process output (${configuration.getName})",
      )
    }

    result
  }

  /**
   * Decorates the console for the separate-process SBT run configuration path.
   *
   * This state extends the plain [[JavaCommandLineState]], whose default console creation is inherited from
   * [[CommandLineState]] and returns an undecorated [[com.intellij.execution.impl.ConsoleViewImpl]].
   * Standard Java application and test states add this decoration in their own `createConsole` implementations.
   * SBT needs to do the same here so Java run configuration extensions and debugger console integrations see the expected console shape.
   *
   * The sbt-shell debug path has a similar requirement, but it is handled separately in
   * [[org.jetbrains.sbt.runner.debugger.MyTrojanRemoteState]]
   * because that path creates a remote attach console through [[com.intellij.debugger.engine.RemoteStateState]]
   * instead of using this state to create the execution console.
   */
  override protected def createConsole(executor: Executor): ConsoleView = {
    val console = super.createConsole(executor)
    if (console == null)
      null
    else
      // Use the same Java console extension pipeline as regular Java run/debug configurations.
      JavaRunConfigurationExtensionManager.getInstance.decorateExecutionConsole(
        configuration,
        getRunnerSettings,
        console,
        executor
      )
  }

  override def createJavaParameters(): OwnJavaParameters = {
    val params = createJavaParametersImpl
    MockSbtProcessForTests.configureJavaParametersForNonSbtShell(configuration.getProject, params)
    params
  }

  private def createJavaParametersImpl: OwnJavaParameters = {
    val project = configuration.getProject
    val params: OwnJavaParameters = new OwnJavaParameters

    params.setWorkingDirectory(configuration.workingDir)

    val sbtExecutionSettings = SbtExternalSystemManager.executionSettingsFor(project)

    val jdk = getJdk(project, sbtExecutionSettings)
    params.configureByProject(project, OwnJavaParameters.JDK_ONLY, jdk)

    val environmentVariables = new util.HashMap(configuration.environmentVariables)
    params.setEnv(environmentVariables)

    val sbtSystemSettings = SbtSettings.getInstance(project).getState

    setClasspathAndMainClass(params, sbtSystemSettings)

    // NOTE: This path passes environment variables to the process, but it does not resolve JAVA_OPTS/.jvmopts
    // or SBT_OPTS/.sbtopts into JVM or sbt launcher arguments.
    // SbtRunner and sbt shell do that through SbtProcessOptionsResolver.
    // TODO: Share the resolver-backed launch model here before adding VM and program parameters.
    params.getVMParametersList.addParametersString(configuration.vmparams)
    params.getProgramParametersList.add(processedCommands)

    params
  }

  private def setClasspathAndMainClass(params: OwnJavaParameters, sbtSystemSettings: SbtSettings.State): Unit = {
    // One of these checks might be redundant.
    // Why do we need the customLauncherEnabled at all?
    if (sbtSystemSettings.customLauncherPath != null) {
      params.getClassPath.add(sbtSystemSettings.customLauncherPath)
      params.setMainClass(determineMainClass(sbtSystemSettings.customLauncherPath))
    } else {
      val launcher = SbtUtil.defaultLauncherPath
      val launcherPath = launcher.toCanonicalPath.toString
      params.getClassPath.add(launcherPath)
      params.setMainClass(determineMainClass(launcherPath))
    }
  }

  private def determineMainClass(launcherPath: String): String = {
    val jar = Path.of(launcherPath)
    JarManifestUtils.readManifestAttribute(jar, "Main-Class").getOrElse("xsbt.boot.Boot")
  }

  private def getJdk(project: Project, sbtExecutionSettings: SbtExecutionSettings): Sdk = {
    val customJdk: Option[Sdk] = for {
      vmExecutablePath <- sbtExecutionSettings.getCustomVMExecutableOrWarn(project)
      // The java installation directory is two levels up.
      // See org.jetbrains.sbt.project.SbtExternalSystemManager.getVmExecutable
      javaHome <- vmExecutablePath << 2
      jdk <- findJdkByHomePath(javaHome.toCanonicalPath.toString)
    } yield jdk

    customJdk.getOrElse {
      JavaParametersUtil.createProjectJdk(project, null)
    }
  }

  private def findJdkByHomePath(homePath: String): Option[Sdk] =
    SdkTable.getInstance.getAllSdks.find { sdk =>
      sdk.getSdkType.isInstanceOf[JavaSdkType] && Option(sdk.getHomePath).exists(home => Path.of(home).toCanonicalPath.toString == homePath)
    }
}
