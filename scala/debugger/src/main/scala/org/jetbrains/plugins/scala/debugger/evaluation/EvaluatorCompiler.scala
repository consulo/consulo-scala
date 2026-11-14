package org.jetbrains.plugins.scala
package debugger
package evaluation

import consulo.container.boot.ContainerPathManager
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.util.ModuleUtilCore
import consulo.module.Module
import consulo.module.content.layer.OrderEnumerator
import org.jetbrains.jps.incremental.scala.shared.{Client, CompileEngine, DummyClient, MessageKind}
import org.jetbrains.plugins.scala.api.NlsString
import org.jetbrains.plugins.scala.compiler.data._
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettings
import org.jetbrains.plugins.scala.project.{ModuleExt, VirtualFileExt}
import org.jetbrains.plugins.scala.util.ScalaPluginJars

import java.nio.file.Path
import scala.annotation.tailrec

/**
 * upstream: the expression is compiled by the Scala compile server (ServerConnector / RemoteServerConnectorBase for
 * `CommandIds.Compile`, compile server Main.evaluateExpressionLogic for `CommandIds.EvaluateExpression`), here it is
 * compiled in the IDE process
 */
private[evaluation] object EvaluatorCompiler {

  type CompileResult = Either[Seq[NlsString], Array[(Path, String)]]

  private def engine: CompileEngine = CompileEngine.fromJpsDirectory(ScalaPluginJars.jpsRoot)

  private lazy val sbtData: Either[String, SbtData] =
    SbtData.from(ScalaPluginJars.jpsRoot, System.getProperty("java.class.version"), ContainerPathManager.get.getSystemDir.resolve("scala-compiler"))

  def compile(module: Module, filesToCompile: Seq[Path], outputDir: Path): CompileResult = {
    val client = new ErrorCollectingClient
    val sbt = sbtData.fold(message => throw new IllegalArgumentException(message), identity)
    engine.compile(sbt, compilerData(module), compilationData(module, filesToCompile, outputDir), client)
    val errors = client.errors
    if (errors.nonEmpty) Left(errors) else Right(classfiles(outputDir))
  }

  def evaluateExpression(arguments: ExpressionEvaluationArguments): Either[Seq[NlsString], Unit] = {
    val client = new ErrorCollectingClient
    engine.evaluateExpression(arguments, client)
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

  private final class ErrorCollectingClient extends DummyClient {
    private val collected = Seq.newBuilder[NlsString]

    override def message(msg: Client.ClientMsg): Unit =
      if (msg.kind == MessageKind.Error) collected += NlsString(msg.text)

    def errors: Seq[NlsString] = collected.result()
  }
}
