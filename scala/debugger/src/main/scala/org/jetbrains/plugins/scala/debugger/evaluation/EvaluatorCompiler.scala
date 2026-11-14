package org.jetbrains.plugins.scala
package debugger
package evaluation

import consulo.container.boot.ContainerPathManager
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.util.ModuleUtilCore
import consulo.module.Module
import consulo.module.content.layer.OrderEnumerator
import org.jetbrains.jps.incremental.scala.local.{Cache, LocalServer}
import org.jetbrains.jps.incremental.scala.shared.{Client, DummyClient, MessageKind}
import org.jetbrains.plugins.scala.api.NlsString
import org.jetbrains.plugins.scala.compiler.data._
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettings
import org.jetbrains.plugins.scala.project.{ModuleExt, VirtualFileExt}
import org.jetbrains.plugins.scala.util.ScalaPluginJars

import java.net.URLClassLoader
import java.nio.file.Path
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

/**
 * upstream: the expression is compiled by the Scala compile server (ServerConnector / RemoteServerConnectorBase for
 * `CommandIds.Compile`, compile server Main.evaluateExpressionLogic for `CommandIds.EvaluateExpression`), here it is
 * compiled in the IDE process
 */
private[evaluation] object EvaluatorCompiler {

  type CompileResult = Either[Seq[NlsString], Array[(Path, String)]]

  private lazy val localServer = new LocalServer

  private lazy val sbtData: Either[String, SbtData] =
    SbtData.from(ScalaPluginJars.jpsRoot, System.getProperty("java.class.version"), ContainerPathManager.get.getSystemDir.resolve("scala-compiler"))

  private val classLoaderCache: Cache[Seq[Path], ClassLoader] = new Cache(3)

  def compile(module: Module, filesToCompile: Seq[Path], outputDir: Path): CompileResult = {
    val client = new ErrorCollectingClient
    val sbt = sbtData.fold(message => throw new IllegalArgumentException(message), identity)
    localServer.compile(sbt, compilerData(module), compilationData(module, filesToCompile, outputDir), client)
    val errors = client.errors
    if (errors.nonEmpty) Left(errors) else Right(classfiles(outputDir))
  }

  def evaluateExpression(arguments: ExpressionEvaluationArguments): Either[Seq[NlsString], Unit] = {
    val client = new ErrorCollectingClient
    runExpressionCompiler(arguments, client)
    val errors = client.errors
    if (errors.nonEmpty) Left(errors) else Right(())
  }

  private def compilerData(module: Module): CompilerData = CompilerData(
    compilerJars = CompilerJarsFactory.fromFiles(module.scalaCompilerClasspath, module.customScalaCompilerBridgeJar, module.replClasspath.asPaths).toOption,
    javaHome = javaHome(module),
    incrementalType = IncrementalityType.IDEA
  )

  private def compilationData(module: Module, filesToCompile: Seq[Path], outputDir: Path): CompilationData = {
    val compilerSettings = ScalaCompilerSettings.forModule(module)
    val sourceRoot = filesToCompile.headOption.flatMap(file => Option(file.toCanonicalPath.getParent))
    CompilationData(
      sources = filesToCompile,
      classpath = runtimeClasspath(module, outputDir),
      output = outputDir,
      scalaOptions = compilerSettings.getOptionsAsStrings(module.hasScala3),
      javaOptions = Seq.empty,
      order = CompileOrder.valueOf(compilerSettings.compileOrder.name),
      cacheFile = Path.of(""),
      outputToCacheMap = Map.empty,
      outputGroups = sourceRoot.map(_ -> outputDir).toSeq,
      zincData = ZincData(
        allSources = Seq.empty,
        compilationStartDate = 0,
        isCompile = false
      )
    )
  }

  // upstream: also the runners, compiler-shared and scala-jps jars of the plugin, needed by worksheets only
  private def runtimeClasspath(module: Module, outputDir: Path): Seq[Path] = {
    val enumerator = OrderEnumerator.orderEntries(module).compileOnly().recursively()
    val classesRoots = enumerator.getClassesRoots.toSeq.map(_.toPath).map(stripJarPathSuffix).map(Path.of(_))
    classesRoots ++ module.scalaCompilerClasspath :+ outputDir
  }

  private def stripJarPathSuffix(path: Path): String =
    path.toCanonicalPath.toString.stripSuffix("!").stripSuffix("!/")

  // upstream: the JDK of the compile server
  private def javaHome(module: Module): Option[Path] =
    Option(ModuleUtilCore.getSdk(module, classOf[JavaModuleExtension[?]]))
      .flatMap(sdk => Option(sdk.getHomePath))
      .map(Path.of(_))

  @tailrec
  private def classfiles(dir: Path, namePrefix: String = ""): Array[(Path, String)] = dir.children().toArray match {
    case Array(d) if d.isDirectory => classfiles(d, s"$namePrefix${d.getFileName}.")
    case files => files.map(f => (f, s"$namePrefix${f.getFileName}".stripSuffix(".class")))
  }

  private def runExpressionCompiler(arguments: ExpressionEvaluationArguments, client: Client): Unit = {
    val ExpressionEvaluationArguments(useBuiltInExpressionCompiler, outDir, classpath, scalacOptions, source, line, expression, localVariableNames, packageName) = arguments

    // upstream: the parent is the system class loader of the compile server, here the IDE classes must stay invisible
    val classLoader = classLoaderCache.getOrUpdate(classpath) { () =>
      new URLClassLoader(classpath.map(_.toUri.toURL).toArray, ClassLoader.getPlatformClassLoader)
    }

    val bridgeClassName =
      if (useBuiltInExpressionCompiler) "dotty.tools.debug.ExpressionCompilerBridge"
      else "dotty.tools.dotc.ExpressionCompilerBridge"

    val bridgeClass = Class.forName(bridgeClassName, true, classLoader)
    val bridgeInstance = bridgeClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
    val runMethod = bridgeClass.getMethods.find(_.getName == "run").get

    val consumer: java.util.function.Consumer[String] = client.error(_)
    val expressionClassName = "CompiledExpression"
    val classpathString = classpath.mkString(java.io.File.pathSeparator)
    val scalacOptionsArray = scalacOptions.toArray
    val localVariableNamesJavaSet = localVariableNames.asJava

    if (useBuiltInExpressionCompiler) {
      val config = expressionCompilerConfig(classLoader, packageName, expressionClassName, line, expression, localVariableNamesJavaSet, consumer)
      runMethod.invoke(bridgeInstance, outDir, classpathString, scalacOptionsArray, source, config)
    } else {
      runMethod.invoke(bridgeInstance, outDir, expressionClassName, classpathString, scalacOptionsArray, source, Int.box(line),
        expression, localVariableNamesJavaSet, packageName, consumer, java.lang.Boolean.FALSE)
    }
  }

  private def expressionCompilerConfig(
    classLoader: ClassLoader,
    packageName: String,
    outputClassName: String,
    breakpointLine: Int,
    expression: String,
    localVariables: java.util.Set[String],
    errorReporter: java.util.function.Consumer[String]
  ): AnyRef = {
    val configClass = Class.forName("dotty.tools.debug.ExpressionCompilerConfig", true, classLoader)

    def invoke(config: AnyRef, name: String, parameterType: Class[?], argument: AnyRef): AnyRef =
      configClass.getDeclaredMethod(name, parameterType).invoke(config, argument)

    var config = configClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
    config = invoke(config, "withPackageName", classOf[String], packageName)
    config = invoke(config, "withOutputClassName", classOf[String], outputClassName)
    config = invoke(config, "withBreakpointLine", classOf[Int], Int.box(breakpointLine))
    config = invoke(config, "withExpression", classOf[String], expression)
    config = invoke(config, "withLocalVariables", classOf[java.util.Set[String]], localVariables)
    config = invoke(config, "withErrorReporter", classOf[java.util.function.Consumer[String]], errorReporter)
    config
  }

  private final class ErrorCollectingClient extends DummyClient {
    private val collected = Seq.newBuilder[NlsString]

    override def message(msg: Client.ClientMsg): Unit =
      if (msg.kind == MessageKind.Error) collected += NlsString(msg.text)

    def errors: Seq[NlsString] = collected.result()
  }
}
