package org.jetbrains.plugins.scala.worksheet.server

import consulo.compiler.{CompilerMessage, CompilerPaths}
import consulo.container.boot.ContainerPathManager
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.util.ModuleUtilCore
import consulo.logging.Logger
import consulo.module.Module
import consulo.module.content.ModuleRootManager
import consulo.module.content.layer.OrderEnumerator
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.annotations.Nls
import org.jetbrains.jps.incremental.scala.shared.{Client, CompileEngine, DelegateClient}
import org.jetbrains.plugins.scala.compiler.data._
import org.jetbrains.plugins.scala.compiler.data.worksheet.WorksheetArgs
import org.jetbrains.plugins.scala.extensions.{LoggerExt, PathExt}
import org.jetbrains.plugins.scala.api.lang.psi.api.ScFile
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettings
import org.jetbrains.plugins.scala.project.{ModuleExt, VirtualFileExt}
import org.jetbrains.plugins.scala.repl.console.configuration.ScalaSdkJLineFixer
import org.jetbrains.plugins.scala.repl.console.configuration.ScalaSdkJLineFixer.JlineResolveResult
import org.jetbrains.plugins.scala.util.ScalaPluginJars
import org.jetbrains.plugins.scala.worksheet.WorksheetUtils
import org.jetbrains.plugins.scala.worksheet.actions.WorksheetFileHook
import org.jetbrains.plugins.scala.worksheet.server.RemoteServerConnector._
import org.jetbrains.plugins.scala.worksheet.settings.WorksheetFileSettings

import java.nio.file.Path
import scala.util.control.NonFatal

/**
 * upstream: sends the worksheet to the Scala compile server (RemoteServerRunner / NonServerRunner), the compile server
 * compiles it and runs it (WorksheetServer). Here both is done in the IDE process by the in-process compile engine
 * ([[CompileEngine.compileAndRunWorksheet]], the same WorksheetServer, see scala/compile-server).
 */
private[worksheet]
final class RemoteServerConnector(
  module: Module,
  worksheetPsiFile: ScFile,
  args: RemoteServerConnector.Args,
  makeType: WorksheetMakeType
) {
  args.compiledFile.foreach(checkFileToCompile)

  private var additionalCompilerClasspath: Seq[Path] = Nil

  private def compilerSettings: ScalaCompilerSettings =
    WorksheetFileSettings(worksheetPsiFile).getCompilerProfile.getSettings

  private def outputDir: Path = args.compilationOutputDir.getOrElse(Path.of(""))

  private def worksheetArgs: Option[WorksheetArgs] =
    makeType match {
      case OutOfProcess => None
      case InProcess =>
        args match {
          case Args.PlainModeArgs(sourceFile, outputDir, className) =>
            Some(WorksheetArgs.RunPlain(
              className,
              RunnersJar,
              sourceFile,
              sourceFile.getFileName.toString,
              outputDir +: outputDirs
            ))
          case Args.ReplModeArgs(path, dropCachedReplInstance, codeChunk) =>
            Some(WorksheetArgs.RunRepl(
              sessionId = path,
              codeChunk,
              dropCachedReplInstance,
              continueOnChunkError = WorksheetUtils.continueWorksheetEvaluationOnExpressionFailure,
              outputDirs
            ))
          case Args.CompileOnly(_, _) =>
            None // just compile (data is taken from CompilationData)
        }
    }

  private def scalaParameters: Seq[String] = {
    val optionsOriginal = compilerSettings.getOptionsAsStrings(module.hasScala3)
    val optionsWithExtra = if (module.hasScala3) {
      val extraOptions = Seq(
        "-color:never", // by default dotty prints lots of color, can't handle for now
      )
      optionsOriginal ++ extraOptions
    } else {
      optionsOriginal
    }
    val optionsWithoutIgnored = optionsWithExtra.filterNot(IgnoredScalacOptions.contains)
    optionsWithoutIgnored
  }

  private def project = module.getProject

  def compileAndRun(
    originalFile: VirtualFile,
    consumer: RemoteServerConnector.WorksheetEvaluation
  )(callback: RemoteServerConnectorResult => Unit): Unit = {
    val client = new MyTranslatingClient(project, originalFile, consumer)
    compileAndRun(originalFile, client)(callback)
  }

  /**
   * Blocks until the worksheet is compiled (and evaluated for [[InProcess]]).
   * The evaluation is stopped by [[WorksheetFileHook.updateStoppableProcess]]: the client is canceled and the thread is
   * interrupted (upstream: the whole compile server process was stopped)
   */
  def compileAndRun(
    originalFile: VirtualFile,
    client: Client
  )(callback: RemoteServerConnectorResult => Unit): Unit = {
    Log.debugSafe(s"compileAndRun: originalFile = $originalFile")

    ScalaSdkJLineFixer.validateJLineInCompilerClassPath(module) match {
      case JlineResolveResult.NotRequired =>
      case JlineResolveResult.RequiredFound(file) =>
        additionalCompilerClasspath = Seq(file)
      case JlineResolveResult.RequiredNotFound =>
        callback(RemoteServerConnectorResult.RequiredJLineIsMissingFromClasspathError(module))
        return
    }

    val thread = Thread.currentThread
    @volatile var stopped = false
    val stoppableClient = new DelegateClient(client) {
      override def isCanceled: Boolean = stopped || super.isCanceled
    }
    // a compilation of the compiler based highlighting is not stopped by the worksheet stop action
    val stoppable = !args.isInstanceOf[Args.CompileOnly]
    if (stoppable) WorksheetFileHook.updateStoppableProcess(originalFile, Some(() => {
      stopped = true
      thread.interrupt()
    }))

    val result = try {
      engine.compileAndRunWorksheet(arguments, stoppableClient)
      RemoteServerConnectorResult.Done
    } catch {
      case _: InterruptedException if stopped =>
        RemoteServerConnectorResult.Done
      case NonFatal(ex) =>
        RemoteServerConnectorResult.ProcessTerminatedError(ex)
    } finally {
      if (stoppable) {
        WorksheetFileHook.updateStoppableProcess(originalFile, None)
        Thread.interrupted()
      }
    }
    callback(result)
  }

  private def arguments: Arguments = {
    val compilerClasspath = module.scalaCompilerClasspath ++ additionalCompilerClasspath
    Arguments(
      sbtData = sbtData.fold(message => throw new IllegalArgumentException(message), identity),
      compilerData = CompilerData(
        compilerJars = CompilerJarsFactory.fromFiles(compilerClasspath, module.customScalaCompilerBridgeJar, module.replClasspath.asPaths).toOption,
        javaHome = javaHome,
        incrementalType = IncrementalityType.IDEA
      ),
      compilationData = CompilationData(
        sources = args.compiledFile.toSeq,
        classpath = runtimeClasspath(compilerClasspath),
        output = outputDir,
        scalaOptions = scalaParameters,
        javaOptions = Seq.empty,
        order = CompileOrder.valueOf(compilerSettings.compileOrder.name),
        cacheFile = Path.of(""),
        outputToCacheMap = Map.empty,
        outputGroups = args.compiledFile.flatMap(file => Option(file.toCanonicalPath.getParent)).map(_ -> outputDir).toSeq,
        zincData = ZincData(
          allSources = Seq.empty,
          compilationStartDate = 0,
          isCompile = false
        )
      ),
      worksheetArgs = worksheetArgs
    )
  }

  // upstream: RemoteServerConnectorBase.runtimeClasspath, also with the compiler-shared and scala-jps jars of the plugin
  private def runtimeClasspath(compilerClasspath: Seq[Path]): Seq[Path] = {
    val enumerator = OrderEnumerator.orderEntries(module).compileOnly().recursively()
    val classesRoots = enumerator.getClassesRoots.toSeq.map(_.toPath).map(stripJarPathSuffix).map(Path.of(_))
    // upstream comment: if `target` directories were deleted manually, they are not returned by the OrderEnumerator,
    // so the output directories are added explicitly
    (classesRoots ++ outputDirs).distinct ++ compilerClasspath :+ RunnersJar :+ outputDir
  }

  private def stripJarPathSuffix(path: Path): String =
    path.toCanonicalPath.toString.stripSuffix("!").stripSuffix("!/")

  // upstream: production or test output of sbt source set modules (separate production and test modules)
  private def outputDirs: Seq[Path] = {
    val modules = ModuleRootManager.getInstance(module).getDependencies :+ module
    modules.toSeq.flatMap(module => Option(CompilerPaths.getModuleOutputPath(module, false))).map(Path.of(_))
  }

  // upstream: the JDK of the compile server
  private def javaHome: Option[Path] =
    Option(ModuleUtilCore.getSdk(module, classOf[JavaModuleExtension[?]]))
      .flatMap(sdk => Option(sdk.getHomePath))
      .map(Path.of(_))

  private def checkFileToCompile(file: Path): Unit =
    if (!file.exists)
      throw new IllegalArgumentException(s"File ${file.toCanonicalPath} does not exists")
}

//private[worksheet]
object RemoteServerConnector {

  private val Log = Logger.getInstance(this.getClass)

  private[worksheet] val RunnersJar: Path = ScalaPluginJars.runnersJar

  private def engine: CompileEngine = CompileEngine.fromJpsDirectory(ScalaPluginJars.jpsRoot)

  // upstream: the compile server system directory
  private lazy val sbtData: Either[String, SbtData] =
    SbtData.from(ScalaPluginJars.jpsRoot, System.getProperty("java.class.version"), ContainerPathManager.get.getSystemDir.resolve("scala-compiler"))

  sealed trait Args {
    final def compiledFile: Option[Path] = this match {
      case Args.PlainModeArgs(sourceFile, _, _) => Some(sourceFile)
      case Args.CompileOnly(sourceFile, _)      => Some(sourceFile)
      case Args.ReplModeArgs(_, _, _)           => None
    }
    final def compilationOutputDir: Option[Path] = this match {
      case Args.PlainModeArgs(_, outputDir, _) => Some(outputDir)
      case Args.CompileOnly(_, outputDir)      => Some(outputDir)
      case _                                   => None
    }
  }
  object Args {
    final case class PlainModeArgs(sourceFile: Path, outputDir: Path, className: String) extends Args
    final case class ReplModeArgs(path: String, dropCachedReplInstance: Boolean, codeChunk: String) extends Args
    /** compiler based highlighting of the worksheet, see [[org.jetbrains.plugins.scala.worksheet.processor.WorksheetHighlightingCompilerImpl]] */
    final case class CompileOnly(sourceFile: Path, outputDir: Path) extends Args
  }

  sealed trait RemoteServerConnectorResult
  object RemoteServerConnectorResult {
    case object Done extends RemoteServerConnectorResult

    sealed trait Error extends RemoteServerConnectorResult
    sealed trait UnhandledError extends Error
    final case class ProcessTerminatedError(cause: Throwable) extends UnhandledError
    final case class ExpectedError(cause: Throwable) extends UnhandledError
    final case class RequiredJLineIsMissingFromClasspathError(module: Module) extends UnhandledError  // (SCL-15818, SCL-15948)
    final case class UnexpectedError(cause: Throwable) extends UnhandledError
  }

  // Worksheet Integration Tests rely on that this is the main entry point for all compiler messages
  trait CompilerMessagesConsumer {
    def message(message: CompilerMessage): Unit
  }

  trait WorksheetEvaluation extends CompilerMessagesConsumer {
    def progress(@Nls text: String, done: Option[Float]): Unit

    def worksheetOutput(text: String): Unit

    def trace(thr: Throwable): Unit

    def isCompiledWithErrors: Boolean

    def start(runnable: Runnable): Unit
  }

  private val IgnoredScalacOptions = Set(
    "-Xfatal-warnings", //before 2.12 and in 2.13
    "-Werror", //since 2.13 only
    "-Wunused:imports", //see SCL-21880
  )
}
