package org.jetbrains.plugins.scala.repl.console.configuration
import com.intellij.java.execution.{CommonJavaRunConfigurationParameters, ShortenCommandLine}
import com.intellij.java.execution.impl.util.JavaParametersUtil
import consulo.execution.{DefaultExecutionResult, ExecutionResult}
import consulo.process.ExecutionException
import consulo.execution.executor.Executor
import consulo.execution.configuration.{ConfigurationFactory, LegacyRunConfigurationConverter, ModuleBasedConfiguration, RunConfigurationModule, RunConfigurationOptions, RunProfileState}
import consulo.execution.configuration.fragment.{EnvironmentFragmentState, ProgramParametersFragmentState, WorkingDirectoryFragmentState}
import consulo.execution.runner.{ExecutionEnvironment, ProgramRunner}
import consulo.java.execution.configurations.OwnJavaParameters
import consulo.java.execution.configurations.fragment.VmParametersFragmentState
import consulo.java.execution.projectRoots.OwnJdkUtil
import consulo.module.Module
import consulo.project.{Project, ProjectPropertiesComponent}
import consulo.content.bundle.Sdk
import consulo.virtualFileSystem.util.PathsList
import org.jdom.Element
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.repl.console.configuration.ScalaSdkJLineFixer.{JlineResolveResult, showJLineMissingNotification}
import org.jetbrains.plugins.scala.repl.console.{ScalaLanguageConsole, ScalaReplBundle}
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.project.*
import org.jetbrains.plugins.scala.api.project.*
import org.jetbrains.plugins.scala.rt.project.*
import org.jetbrains.plugins.scala.shared.project.*
import org.jetbrains.plugins.scala.runner.{ScalaTargetAwareCommandLineState, ScalaTargetAwareRunConfiguration}
import org.jetbrains.plugins.scala.util.JdomExternalizerMigrationHelper

import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.util.chaining.scalaUtilChainingOps

/**
 * Run configuration with a single purpose: run Scala REPL instance in a internal IDEA console.
 * <br>
 * The class is not intended to be reused/extended in other plugins.
 * If you want to reuse some of the class functionality, please contact Scala Plugin team
 * via https://gitter.im/JetBrains/intellij-scala and we will extract some proper abstract base class.
 *
 * upstream: settings editor form; in Consulo the settings are edited by the platform run configuration fragments
 * (module, program arguments = console arguments, VM options, working directory, environment variables)
 */
// upstream: @ApiStatus.Experimental
class ScalaConsoleRunConfiguration(
  project: Project,
  configurationFactory: ConfigurationFactory,
  name: String
) extends ScalaTargetAwareRunConfiguration[RunConfigurationModule, Element](
  name,
  new RunConfigurationModule(project),
  configurationFactory
) with CommonJavaRunConfigurationParameters {

  //language=Scala
  private val Scala2MainClass = "scala.tools.nsc.MainGenericRunner"
  //language=Scala
  private val Scala3MainClass = "dotty.tools.repl.Main"

  private val UseJavaCp = "-usejavacp"

  def consoleArgs: String = ensureUsesJavaCpByDefault(getProgramParameters)

  def workingDirectory: String = Option(getWorkingDirectory).getOrElse(Option(getProject.baseDir).map(_.getPath).getOrElse(""))

  def javaOptions: String = Option(getVMParameters).getOrElse("")

  private def ensureUsesJavaCpByDefault(args: String): String =
    if (args == null || args.isEmpty) UseJavaCp
    else args

  private def getModule: Option[Module] = Option(getConfigurationModule.getModule)

  private def requireModule: Module = getModule.getOrElse(throw new ExecutionException(ScalaReplBundle.message("scala.console.config.module.is.not.specified")))

  override def getValidModules: java.util.Collection[Module] = getProject.modulesWithScala.toList.asJava

  override def getRunClass: String = null

  override def getPackage: String = null

  override protected def getOptionsClass: Class[? <: RunConfigurationOptions] = classOf[RunConfigurationOptions]

  override protected def convertLegacyState(converter: LegacyRunConfigurationConverter): Unit = {
    super.convertLegacyState(converter)
    val element = converter.getElement

    def option(name: String): Option[String] =
      element.getChildren("option").asScala
        .find(o => name == o.getAttributeValue("name"))
        .flatMap(o => Option(o.getAttributeValue("value")))

    var consoleArgs = option("myConsoleArgs")
    var workingDirectory = option("workingDirectory")
    var javaOptions = option("javaOptions")
    JdomExternalizerMigrationHelper(element) { helper =>
      helper.migrateString("consoleArgs")(v => consoleArgs = Some(v))
      helper.migrateString("workingDirectory")(v => workingDirectory = Some(v))
      helper.migrateString("javaOptions")(v => javaOptions = Some(v))
      // see revision 8a3f9d28c, some time ago javaOptions was serialized as "vmparams4"
      helper.migrateString("vmparams4")(v => javaOptions = Some(v))
    }

    val envs = new java.util.LinkedHashMap[String, String]()
    for {
      envOption <- element.getChildren("option").asScala.find(o => "environmentVariables" == o.getAttributeValue("name"))
      map <- Option(envOption.getChild("map"))
      entry <- map.getChildren("entry").asScala
      key <- Option(entry.getAttributeValue("key"))
    } envs.put(key, Option(entry.getAttributeValue("value")).getOrElse(""))

    converter.addFragment(new ProgramParametersFragmentState(ensureUsesJavaCpByDefault(consoleArgs.orNull)))
    converter.addFragment(new WorkingDirectoryFragmentState(workingDirectory.orNull))
    converter.addFragment(new VmParametersFragmentState(javaOptions.orNull))
    converter.addFragment(new EnvironmentFragmentState(envs, true))
  }

  override def getState(executor: Executor, env: ExecutionEnvironment): RunProfileState =
    new ScalaCommandLineState(env)

  //overriding the method as a workaround for https://github.com/lampepfl/dotty/issues/19007
  override def clone(): ModuleBasedConfiguration[?] = super.clone()

  private class ScalaCommandLineState(env: ExecutionEnvironment) extends ScalaTargetAwareCommandLineState(env) {
    getModule match {
      case Some(module) =>
        setConsoleBuilder(ScalaLanguageConsole.builderFor(module))
      case None =>
    }

    override protected def createJavaParameters: OwnJavaParameters = {
      val params = createParams
      val module = requireModule
      params.getProgramParametersList.addParametersString(consoleArgs)

      //Without this option JLine won't be able to create a terminal
      //And in Scala 3 there is no way to disable JLine like in Scala so we just set it in "dumb" mode
      //see dotty.tools.repl.JLineTerminal.dumbTerminal (in scala3-compiler_3-3.0.0.jar)
      //Also see related: IDEA-183619
      if (module.hasScala3)
        params.addEnv("TERM", "dumb")
      else
        params.getProgramParametersList.addParametersString(disableJLineOption)

      params
    }

    /**
     * @note Written according to [[com.intellij.execution.JavaTestFrameworkRunnableState#execute]].
     */
    override def execute(executor: Executor, runner: ProgramRunner[?]): ExecutionResult = {
      val params: OwnJavaParameters = getJavaParameters
      val classPath = params.getClassPath

      val module = requireModule
      val success = ensureJLineInClassPathOrShowErrorNotification(classPath, module, ScalaLanguageConsole.ScalaConsoleTitle)
      if (!success)
        return null

      val processHandler = createHandler()

      val console = createConsole(executor)
      if (console != null) {
        console.attachToProcess(processHandler)
      }

      new DefaultExecutionResult(console, processHandler)
    }

    private def ensureJLineInClassPathOrShowErrorNotification(classPathList: PathsList, module: Module, @Nls subsystemName: String): Boolean = {
      val classPath = classPathList.getPathList.asScala.map(Path.of(_)).toSeq
      val result = ScalaSdkJLineFixer.validateJLineInClassPath(classPath, module)
      result match {
        case JlineResolveResult.NotRequired =>
          true
        case JlineResolveResult.RequiredFound(file) =>
          classPathList.add(file.toFile)
          true
        case JlineResolveResult.RequiredNotFound =>
          showJLineMissingNotification(module, subsystemName)
          false
      }
    }
  }

  private def disableJLineOption: String = {
    import ScalaConsoleRunConfiguration.{Scala2_13_14, Scala2_13_2}
    val scalaVersion: Option[ScalaVersion] = getModule.flatMap(_.scalaMinorVersion)
    if (scalaVersion.exists(v => Scala2_13_2 <= v && v < Scala2_13_14)) {
      // https://github.com/scala/scala/pull/8906
      // https://github.com/scala/scala/pull/10716
      "-Xjline:off"
    } else
      "-Xnojline"
  }

  private def addScalaCompilerClassPath(classPath: PathsList, module: Module): Unit = {
    val compilerClasspath = module.scalaCompilerClasspath.map(_.toCanonicalPath.toString)
    classPath.addAll(compilerClasspath.asJava)
  }

  private def createParams: OwnJavaParameters = {
    val module = requireModule

    // upstream: always uses the module SDK (no alternative JRE)
    val jdk = JavaParametersUtil.createModuleJdk(module, false, getAlternativeJrePath)

    new OwnJavaParameters().tap { params =>
      addScalaCompilerClassPath(params.getClassPath, module)
      module.replClasspath match {
        case ReplClasspath.Bundled =>
        case ReplClasspath.Provided(classpath) =>
          params.getClassPath.addAll(classpath.map(_.toString).asJava)
      }

      params.configureByModule(module, OwnJavaParameters.JDK_AND_CLASSES_AND_TESTS, jdk)

      params.getVMParametersList.addParametersString(javaOptions)
      params.setShortenCommandLine(getShortenCommandLineMethod(Option(params.getJdk)), project)
      params.setWorkingDirectory(workingDirectory)

      // upstream: also reads environment variables from env files
      params.setEnv(new java.util.HashMap(getEnvs))
      params.setPassParentEnvs(isPassParentEnvs)

      val mainClass = if (module.hasScala3) Scala3MainClass else Scala2MainClass
      params.setMainClass(mainClass)
    }
  }

  /** ShortenCommandLine.ARGS_FILE is intentionally not used even if JdkUtil.useClasspathJar is true
   * Scala REPL does not work in JDK 8 with manifest classpath
   *
   * @see [[com.intellij.execution.ShortenCommandLine.getDefaultMethod]]
   */
  private def getShortenCommandLineMethod(jdk: Option[Sdk]): ShortenCommandLine =
    if (!useDynamicClasspath(getProject)) {
      ShortenCommandLine.NONE
    } else if (safeMap(jdk)(_.getHomePath).exists(OwnJdkUtil.isModularRuntime(_: String))) {
      ShortenCommandLine.ARGS_FILE
    } else {
      ShortenCommandLine.CLASSPATH_FILE
    }

  // upstream: JdkUtil.useDynamicClasspath
  private def useDynamicClasspath(project: Project): Boolean =
    ProjectPropertiesComponent.getInstance(project).getBoolean("dynamic.classpath", java.lang.Boolean.getBoolean("idea.dynamic.classpath"))

  @inline
  private def safeMap[A, B](oa: Option[A])(f: A => B): Option[B] =
    oa.flatMap(a => Option(f(a)))
}

object ScalaConsoleRunConfiguration {
  private val Scala2_13_2 = ScalaVersion.Latest.Scala_2_13.withMinor(2)
  private val Scala2_13_14 = ScalaVersion.Latest.Scala_2_13.withMinor(14)
}
