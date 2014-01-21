package org.jetbrains.jps.incremental.scala
package local

import sbt.compiler.{CompilerCache, CompilerArguments, CompileOutput, AnalyzingCompiler}
import org.jetbrains.jps.incremental.scala.data.CompilationData
import java.io.File
import xsbti.compile.DependencyChanges
import xsbti.{Position, Severity}
import xsbti.api.SourceAPI

/**
 * Nikolay.Tropin
 * 11/18/13
 */
class IdeaIncrementalCompiler(scalac: AnalyzingCompiler) extends AbstractCompiler {
  def compile(compilationData: CompilationData, client: Client): Unit = {
    val progress = getProgress(client)
    val reporter = getReporter(client)
    val logger = getLogger(client)
    val clientCallback = new ClientCallback(client)

    val out = CompileOutput(compilationData.outputGroups: _*)
    val cArgs = new CompilerArguments(scalac.scalaInstance, scalac.cp)
    val options = "IntellijIdea.simpleAnalysis" +: cArgs(Nil, compilationData.classpath, None, compilationData.scalaOptions)

    try scalac.compile(compilationData.sources, emptyChanges, options, out, clientCallback, reporter, CompilerCache.fresh, logger, Option(progress))
    catch {
      case _: xsbti.CompileFailed => // the error should be already handled via the `reporter`
    }
  }

}

private class ClientCallback(client: Client) extends ClientCallbackBase {

  override def generatedClass(source: File, module: File, name: String) {
    client.generated(source, module, name)
  }

  override def endSource(source: File) {
    client.processed(source)
  }
}

abstract class ClientCallbackBase extends xsbti.AnalysisCallback {
  def beginSource(source: File): Unit = {}
  def sourceDependency(dependsOn: File, source: File, publicInherited: Boolean): Unit = {}
  def binaryDependency(binary: File, name: String, source: File, publicInherited: Boolean): Unit = {}
  def generatedClass(source: File, module: File, name: String): Unit = {}
  def endSource(sourcePath: File): Unit = {}
  def api(sourceFile: File, source: SourceAPI): Unit = {}
  def problem(what: String, pos: Position, msg: String, severity: Severity, reported: Boolean): Unit = {}
}

private object emptyChanges extends DependencyChanges {
  val modifiedBinaries = new Array[File](0)
  val modifiedClasses = new Array[String](0)
  def isEmpty = true
}