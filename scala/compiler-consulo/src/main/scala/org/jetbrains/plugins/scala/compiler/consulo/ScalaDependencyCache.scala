package org.jetbrains.plugins.scala.compiler.consulo

import consulo.compiler.{CacheCorruptedException, CompileContext, CompileContextEx, DependencyCache}
import consulo.module.Module
import consulo.module.content.util.ModuleContentUtil
import consulo.util.lang.{Pair, Trinity}
import consulo.util.lang.ref.SimpleReference
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.project.ModuleExt

import java.io.File
import java.nio.file.{Files, Path}
import java.util.function.Function
import java.util as ju
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

/**
 * Consulo only (sbt incrementality): the Consulo compiler calls [[ScalaCompiler]] only for module chunks with changed
 * files, so after a Scala module was compiled, one source of every dependent Scala module is reported as dependent.
 * zinc then recompiles what the API changes of the upstream module invalidate (upstream: JPS runs the builders of
 * all chunks).
 */
final class ScalaDependencyCache extends DependencyCache {

  private val changedModules = mutable.LinkedHashSet.empty[Module]

  private[consulo] def moduleCompiled(modules: Seq[Module]): Unit = synchronized {
    changedModules ++= modules
  }

  override def findDependentFiles(context: CompileContext,
                                  exceptionRef: SimpleReference[CacheCorruptedException],
                                  filter: Function[Pair[Array[Int], ju.Set[Path]], Pair[Array[Int], ju.Set[Path]]],
                                  dependentFiles: ju.Set[Path],
                                  compiledWithErrors: ju.Set[Path]): Unit = {
    val changed = synchronized {
      val result = changedModules.toSeq
      changedModules.clear()
      result
    }
    if (changed.isEmpty) return

    val affected = context.getCompileScope.getAffectedModules.toSet
    val dependents = inReadAction {
      val result = new ju.LinkedHashSet[Module]()
      changed.foreach(ModuleContentUtil.collectModulesDependsOn(_, result))
      result.asScala.toSeq.filter(m => !changed.contains(m) && affected.contains(m) && m.hasScala)
    }

    dependents.foreach { module =>
      val sources = inReadAction(Seq(false, true).flatMap(isTests => BuildTarget(Seq(module), isTests).allSources(context)))
      triggerSource(sources).foreach(dependentFiles.add)
    }
  }

  // the smallest source: zinc treats it as changed and recompiles it
  private def triggerSource(sources: Seq[Path]): Option[Path] =
    sources.filter(Files.isRegularFile(_)).minByOption(Files.size)

  override def hasUnprocessedTraverseRoots: Boolean = synchronized(changedModules.nonEmpty)

  override def resetState(): Unit = synchronized(changedModules.clear())

  override def clearTraverseRoots(): Unit = ()

  override def update(): Unit = ()

  override def relativePathToQName(path: String, separator: Char): String = null

  override def syncOutDir(trinity: Trinity[File, String, java.lang.Boolean]): Unit = ()
}

private object ScalaDependencyCache {

  def in(context: CompileContext): Option[ScalaDependencyCache] = context match {
    case contextEx: CompileContextEx =>
      try Option(contextEx.getDependencyCache.findChild(classOf[ScalaDependencyCache]))
      catch {
        case _: IllegalArgumentException => None
      }
    case _ => None
  }
}
