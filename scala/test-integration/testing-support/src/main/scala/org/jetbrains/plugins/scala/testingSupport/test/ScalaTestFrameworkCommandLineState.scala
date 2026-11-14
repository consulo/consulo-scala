package org.jetbrains.plugins.scala.testingSupport.test

import com.intellij.java.execution.impl.RunConfigurationExtension
import com.intellij.java.execution.impl.util.JavaParametersUtil
import consulo.java.execution.configurations.OwnJavaParameters
import consulo.process.cmd.ParametersList
import consulo.execution.runner.{ExecutionEnvironment, ProgramRunner}
import consulo.execution.test.sm.SMTestRunnerConnectionUtil
import consulo.execution.test.ui.BaseTestsOutputConsoleView
import consulo.execution.ExecutionResult
import consulo.execution.executor.Executor
import consulo.application.{ApplicationManager, ReadAction}
import consulo.dataContext.DataContext
import consulo.language.editor.LangDataKeys
import consulo.module.Module
import consulo.pathMacro.{Macro, MacroManager}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import consulo.content.bundle.Sdk
import consulo.disposer.Disposer
import consulo.util.lang.StringUtil
import consulo.ui.ex.awt.UIUtil
import consulo.process.local.EnvironmentUtil
import consulo.virtualFileSystem.util.PathsList
import org.apache.commons.lang3.StringUtils
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.project.{ModuleExt, ProjectExt}
import org.jetbrains.plugins.scala.runner.ScalaTargetAwareCommandLineState
import org.jetbrains.plugins.scala.testingSupport.test.CustomTestRunnerBasedStateProvider.TestFrameworkRunnerInfo
import org.jetbrains.plugins.scala.testingSupport.test.ScalaTestFrameworkCommandLineState._
import org.jetbrains.plugins.scala.testingSupport.test.exceptions.executionException
import org.jetbrains.plugins.scala.testingSupport.test.testdata.TestConfigurationData
import org.jetbrains.plugins.scala.testingSupport.test.utils.{JavaParametersModified, RawProcessOutputDebugLogger}
import org.jetbrains.plugins.scala.testrunners.testingSupport.TestRunnerUtil
import org.jetbrains.plugins.scala.util.ScalaPluginJars
import org.jetbrains.plugins.scala.extensions.PathExt
import consulo.util.io.ClassPathUtil

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.{util => ju}
import scala.jdk.CollectionConverters._

/**
 * For ScalaTest, Spec2, uTest
 *
 * @param failedTests if defined, the list of test classes and methods to run
 *                    is taken from it instead of from test data
 */
// upstream: @ApiStatus.Internal
class ScalaTestFrameworkCommandLineState(
  override val configuration: AbstractTestRunConfiguration,
  env: ExecutionEnvironment,
  override val failedTests: Option[Seq[(String, String)]],
  runnerInfo: TestFrameworkRunnerInfo
) extends ScalaTargetAwareCommandLineState(env)
  with ScalaTestFrameworkCommandLineStateLike {

  override def createJavaParameters(): OwnJavaParameters = {
    val params = new JavaParametersModified()

    params.setCharset(null)

    val envs: Map[String, String] = VariablesExpander.getEnvVariablesExpanded(testConfigurationData)
    params.setEnv(envs.asJava)
    params.setPassParentEnvs(testConfigurationData.isPassParentEnvs)

    val vmParams: String = VariablesExpander.expandVmOptions(testConfigurationData.javaOptions, envs)
    params.getVMParametersList.addParametersString(vmParams)

    // upstream: JavaRunConfigurationExtensionManager.updateJavaParameters (filters non-applicable extensions)
    RunConfigurationExtension.EP_NAME.getExtensionList.forEach(_.updateJavaParameters(configuration, params, getRunnerSettings))

    if (DebugOptions.attachDebugAgent) {
      val suspend = if(DebugOptions.waitUntilDebuggerAttached) "y" else "n"
      params.getVMParametersList.addParametersString(s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=$suspend,address=${DebugOptions.port}")
    }

    val workingDirEffective = VariablesExpander.getWorkingDirExpanded(configuration)
    params.setWorkingDirectory(workingDirEffective)

    addRunnersJar(params.getClassPath)
    params.setMainClass(runnerInfo.runnerClass)

    // upstream: hack fix for SCL-12564 (increments the structural modification count of PersistentFSImpl)

    // upstream: ProjectJdkTable.findJdk(path), the Consulo JRE fragment stores an SDK name or a JRE home path
    val maybeCustomSdk = testConfigurationData.jrePath.toOption.filter(_.nonEmpty).map(JavaParametersUtil.createProjectJdk(project, _))

    if (testConfigurationData.searchTestsInWholeProject) {
      def anyJdk: Option[Sdk] = {
        val modules = project.modules.iterator.filterNot(_.isBuildModule)
        modules.map(OwnJavaParameters.getValidJdkToRunModule(_, false)).nextOption()
      }
      val sdk = maybeCustomSdk.orElse(anyJdk)
      // TODO: handle case if  project contains multiple scala versions, runtime contains garbage with multiple versions
      params.configureByProject(project, OwnJavaParameters.JDK_AND_CLASSES_AND_TESTS, sdk.orNull)
    } else {
      val sdk = maybeCustomSdk.getOrElse(OwnJavaParameters.getValidJdkToRunModule(module, false))
      params.configureByModule(module, OwnJavaParameters.JDK_AND_CLASSES_AND_TESTS, sdk)
    }

    val suitesToTestsMap = buildSuitesToTestsMap
    val programParameters = buildProgramParameters(suitesToTestsMap)
    val useTestsArgsFile = {
      // multiple test classes can blow command line length
      // it can be observed when running tests in "all in package", "all in project" mode
      val hasMultipleTestsClasses = suitesToTestsMap.size > 1
      hasMultipleTestsClasses
    }
    if (useTestsArgsFile) {
      val argsFile = prepareTempArgsFile(programParameters.testsArgs)
      params.getProgramParametersList.add(s"@${argsFile.toAbsolutePath.toString}")
      params.getProgramParametersList.addAll(programParameters.otherArgs*)
    } else {
      params.getProgramParametersList.addAll(programParameters.allArgs*)
    }

    params.setShortenCommandLine(configuration.getShortenCommandLine, project)

    params
  }

  /**
   * upstream: adds ScalaPluginJars.runnersJar (runners, test-runners and test-runners-spec2_2x merged into one jar),
   * transferred to the remote eel environment if needed.
   * In Consulo the test runners are separate jars of the plugin, see [[ScalaTestFrameworkCommandLineState.runnersClassPath]]
   */
  private def addRunnersJar(classPath: PathsList): Unit =
    runnersClassPath.foreach(jar => classPath.add(jar.toString))

  /**
   * Process command line has length limitation (on windows it's max 32768 characters).
   * When we run many tests (e.g. in "all in package", "all in project" mode) we can potentially pass a lot of
   * command line parameters to test runners which will lead to exception during process creation.
   *
   * We could use [[ShortenCommandLine]] to solve the issue, but it only focuses on shortening of a classpath.<br>
   * Even [[ShortenCommandLine.ARGS_FILE @argfile]] only shortens classpath (IDEA-249722). Besides, it works with Java 9+ only.
   *
   * To support running many tests with any JDK and with any shortening mode we use custom arg file feature.
   */
  private def prepareTempArgsFile(
    programParameters: Seq[String]
  ): Path = try {
    val tempFile = Files.createTempFile("idea_scala_test_runner", ".tmp")
    Files.write(tempFile, programParameters.asJava, StandardCharsets.UTF_8)
    tempFile
  } catch {
    case e: IOException =>
      throw executionException("Failed to create temporary tests args file", e)
  }

  private def buildProgramParameters(suitesToTests: Map[String, Set[String]]): ScalaTestRunnerProgramArgs = {
    val classesAndTests = buildClassesAndTestsParameters(suitesToTests)
    val progress = Seq("-showProgressMessages", testConfigurationData.showProgressMessages.toString)
    val other = ParametersList.parse(testConfigurationData.testArgs)
    ScalaTestRunnerProgramArgs(
      classesAndTests,
      (progress ++ other).map(VariablesExpander.expandProgramArgument)
    )
  }

  private def buildClassesAndTestsParameters(suitesToTests: Map[String, Set[String]]): Seq[String] = {
    val classKey = "-s"
    val testNameKey = "-testName"

    suitesToTests.flatMap { case (className, tests) =>
      val classParams = Seq(classKey, sanitize(className))
      val testsParams = tests.toSeq.filter(StringUtils.isNotBlank).flatMap(Seq(testNameKey, _))
      classParams ++ testsParams
    }.toSeq
  }

  /**
   * @note Written according to [[com.intellij.execution.JavaTestFrameworkRunnableState#execute]].
   */
  override def execute(executor: Executor, runner: ProgramRunner[?]): ExecutionResult = {
    val testConsoleView: BaseTestsOutputConsoleView = {
      val consoleProperties = configuration.createTestConsoleProperties(executor)
      consoleProperties.setIdBasedTestTree(true)
      UIUtil.invokeAndWaitIfNeeded(() => SMTestRunnerConnectionUtil.createConsole("Scala", consoleProperties))
    }
    // upstream: decorates the console with JavaRunConfigurationExtensionManager.decorateExecutionConsole (e.g. profiler
    // widgets), Consulo has no console decorators
    Disposer.register(configuration.getProject, testConsoleView)

    val processHandler = createHandler()
    attachExtensionsToProcess(configuration, processHandler)
    RawProcessOutputDebugLogger.maybeAddListenerTo(processHandler)

    testConsoleView.attachToProcess(processHandler)

    createExecutionResult(testConsoleView, testConsoleView, processHandler)
  }

  /**
   * @see [[org.jetbrains.plugins.scala.testingSupport.test.ui.CommonScalaParametersPanel.isMacroSupportEnabled]]
   * @see [[org.jetbrains.plugins.scala.testingSupport.test.ui.CommonScalaParametersPanel.initMacroSupport]]
   */
  protected object VariablesExpander {
    import consulo.execution.util.ProgramParametersUtil
    /** Expands all kinds of path variables/macro in working directory, examples: $PROJECT_DIR$, $MODULE_WORKING_DIR$, etc... */
    def getWorkingDirExpanded(configuration: AbstractTestRunConfiguration): String =
      ProgramParametersUtil.getWorkingDir(configuration.testConfigurationData, project, module)

    def expandProgramArgument(arg: String): String =
      expandPathAndMacros(arg)

    /** expands path, macro and env variables */
    def expandVmOptions(vmOptions: String, envs: Map[String, String]): String = {
      val expandedPaths = expandPathAndMacros(vmOptions)
      //injection of environment variables to VM options was added within SCL-4812
      // I am not sure why, because no one asked for that, and it doesn't work for any other Run Configuration ATM
      val expandedEnvs = expandEnvs(expandedPaths, envs)
      expandedEnvs
    }

    /**
     * Expands parents environment variables and paths <br>
     * Similar logic is located in [[com.intellij.execution.util.ProgramParametersConfigurator#configureConfiguration]]<br>
     * It might be Exported to some utility method in [[ProgramParametersUtil]]
     */
    def getEnvVariablesExpanded(configuration: TestConfigurationData): Map[String, String] = {
      val envs = new ju.HashMap[String, String](configuration.getEnvs)
      // upstream: also reads environment variables from .env files
      if (configuration.isPassParentEnvs) {
        EnvironmentUtil.inlineParentOccurrences(envs)
      }
      envs.asScala.view.mapValues(ProgramParametersUtil.expandPath(_, module, project)).toMap
    }

    // upstream: ProgramParametersUtil.expandPathAndMacros (the platform one expands only path macros)
    private def expandPathAndMacros(text: String): String = {
      val path = ProgramParametersUtil.expandPath(text, module, project)
      if (path == null || !path.contains("$")) path
      else {
        val dataContext = DataContext.builder()
          .add(VirtualFile.KEY, project.getBaseDir)
          .add(Project.KEY, project)
          .add(Project.PROJECT_FILE_DIRECTORY, project.getBaseDir)
          .add(Module.KEY, module)
          .add(LangDataKeys.MODULE_CONTEXT, module)
          .build()
        try {
          val expanded = ReadAction.compute[String, Macro.ExecutionCancelledException] { () =>
            MacroManager.getInstance().expandMacrosInString(path, false, dataContext)
          }
          if (expanded == null) path else expanded
        } catch {
          case _: Macro.ExecutionCancelledException => path
        }
      }
    }
  }
}

object ScalaTestFrameworkCommandLineState {

  /**
   * test-runners and test-runners-spec2_2x jars (lib/runners in the plugin directory), added to the classpath of the tests.
   * test-runners is also a library of the plugin (the IDE uses TestRunnerUtil), test-runners-spec2_2x is not loaded by the IDE.
   */
  private[test] def runnersClassPath: Seq[Path] = {
    val testRunnersJar = Path.of(ClassPathUtil.getJarPathForClass(classOf[TestRunnerUtil]))
    val specs2xJar = ScalaPluginJars.libRoot / "runners" / "test-runners-spec2_2x.jar"
    Seq(testRunnersJar, specs2xJar)
  }

  private def expandEnvs(text: String, envs: collection.Map[String, String]): String =
    envs.foldLeft(text) { case (text, (key, value)) =>
      StringUtil.replace(text, "$" + key + "$", value, false)
    }

  // ScalaTest does not understand backticks in class/package qualified names, it will fail to run
  private def sanitize(qualifiedName: String): String = qualifiedName.replace("`", "")

  private case class ScalaTestRunnerProgramArgs(
    testsArgs: Seq[String],
    otherArgs: Seq[String]
  ) {
    def allArgs: Seq[String] = testsArgs ++ otherArgs
  }
}
