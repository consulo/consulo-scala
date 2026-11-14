package org.jetbrains.jps.incremental.scala.shared

import org.jetbrains.plugins.scala.compiler.data.{CompilationData, CompilerData, ExpressionEvaluationArguments, SbtData}

import java.net.URLClassLoader
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * upstream: the Scala compile server process (org.jetbrains.jps.incremental.scala.remote.Main), here zinc runs in the IDE
 * process. The implementation (compile-server.jar) and zinc with its Scala 2.13 libraries (incremental-compiler.jar)
 * are loaded from the plugin `lib/jps` directory in their own class loader, see [[CompileEngine.fromJpsDirectory]]
 */
trait CompileEngine {

  /** Throws if the compilation could not be performed, compilation errors are reported to the client */
  def compile(sbtData: SbtData, compilerData: CompilerData, compilationData: CompilationData, client: Client): Unit

  /** Scala 3 expression compiler of the debugger, errors are reported to the client */
  def evaluateExpression(arguments: ExpressionEvaluationArguments, client: Client): Unit
}

object CompileEngine {

  private val ImplementationClassName = "org.jetbrains.jps.incremental.scala.local.CompileEngineImpl"

  private val engines = new ConcurrentHashMap[Seq[Path], CompileEngine]()

  /** upstream: the compile server classpath (CompileServerLauncher.compileServerJars) */
  def jars(pluginJpsDirectory: Path): Seq[Path] = Seq(
    "compiler-interface.jar",
    "sbt-interface.jar",
    "incremental-compiler.jar",
    "compile-server.jar"
  ).map(pluginJpsDirectory.resolve)

  def fromJpsDirectory(pluginJpsDirectory: Path): CompileEngine =
    engines.computeIfAbsent(jars(pluginJpsDirectory), create)

  /**
   * The parent is the plugin class loader: the Scala standard library, compiler-shared and the platform classes come
   * from there. scala-parallel-collections (needed by zinc) defines classes in packages of the scala.library module,
   * so it can be loaded only outside the plugin module layer.
   */
  private def create(jars: Seq[Path]): CompileEngine = {
    val loader = new URLClassLoader("scala-compile-engine", jars.map(_.toUri.toURL).toArray, classOf[CompileEngine].getClassLoader)
    loader.loadClass(ImplementationClassName).getDeclaredConstructor().newInstance().asInstanceOf[CompileEngine]
  }
}
