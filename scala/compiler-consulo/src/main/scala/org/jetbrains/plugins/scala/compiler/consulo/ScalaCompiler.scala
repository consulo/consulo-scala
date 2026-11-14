package org.jetbrains.plugins.scala.compiler.consulo

import com.intellij.java.language.impl.{JavaClassFileType, JavaFileType}
import consulo.annotation.component.ExtensionImpl
import consulo.compiler.scope.CompileScope
import consulo.compiler.{CompileContext, CompileContextEx, TranslatingCompiler}
import consulo.localize.LocalizeValue
import consulo.module.Module
import consulo.project.Project
import consulo.util.collection.Chunk
import consulo.virtualFileSystem.fileType.FileType
import jakarta.inject.Inject
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration

import java.nio.file.{Files, Path}
import java.util.function.Consumer
import java.util as ju
import scala.jdk.CollectionConverters.*

/**
 * Consulo-specific: compiles Scala (and, with the sbt incrementality type, the Java sources of Scala modules) inside the
 * IDE process with zinc. upstream: the JPS builders `SbtBuilder` / `IdeaIncrementalBuilder` running in the JPS process.
 */
@ExtensionImpl(id = "scala-compiler", order = "before java-compiler")
final class ScalaCompiler @Inject()(project: Project) extends TranslatingCompiler {

  override def getDescription: String = LocalizeValue.localizeTODO("Scala compiler").get

  override def validateConfiguration(scope: CompileScope): Boolean = true

  override def registerCompilableFileTypes(consumer: Consumer[FileType]): Unit =
    consumer.accept(ScalaFileType.INSTANCE)

  override def getInputFileTypes: Array[FileType] = Array(ScalaFileType.INSTANCE, JavaFileType.INSTANCE)

  override def getOutputFileTypes: Array[FileType] = Array(JavaClassFileType.INSTANCE)

  override def isCompilableFile(file: Path, context: CompileContext): Boolean = {
    val name = file.getFileName.toString
    if (name.endsWith(ScalaCompiler.ScalaExtension)) true
    else if (name.endsWith(ScalaCompiler.JavaExtension)) context.getModuleByFile(file) match {
      case null => false
      case module => zincCompilesJava && inReadAction(module.hasScala)
    }
    else false
  }

  override def compile(context: CompileContext, moduleChunk: Chunk[Module], files: ju.Collection[Path], sink: TranslatingCompiler.OutputSink): Unit = {
    val modules = moduleChunk.getNodes.asScala.toSeq.sortBy(_.getName)
    val incrementality = ScalaCompilerConfiguration.instanceIn(project).incrementalityType
    val contextEx = context.asInstanceOf[CompileContextEx]
    val dirtyFiles = files.asScala.toSeq.map(_.toAbsolutePath.normalize())

    if (!inReadAction(modules.exists(_.hasScala))) {
      if (dirtyFiles.exists(_.getFileName.toString.endsWith(ScalaCompiler.ScalaExtension)))
        context.newWarning(LocalizeValue.localizeTODO(s"No Scala SDK in module(s) ${modules.map(_.getName).mkString(", ")}, Scala files are skipped")).add()
      return
    }

    // production first: the test target has the production output in its classpath
    for (isTests <- Seq(false, true) if !context.getProgressIndicator.isCanceled) {
      val target = BuildTarget(modules, isTests)
      val targetDirty = dirtyFiles.filter(contextEx.isInTestSourceContent(_) == isTests)
      val allSources = inReadAction(target.allSources(context))
      val dirty = incrementality match {
        // upstream: IdeaIncrementalBuilder marks the package objects of changed base class sources dirty
        case IncrementalityType.IDEA if !context.isRebuild =>
          val allSourcesSet = allSources.toSet
          val packageObjects = ScalaBuilder.packageObjectsData(project, context)
            .invalidatedPackageObjects(targetDirty.toSet)
            .filter(file => allSourcesSet.contains(file) && Files.exists(file))
          targetDirty ++ packageObjects.toSeq.sorted
        case _ => targetDirty
      }
      val run = incrementality match {
        // zinc finds the changes itself: it must also run for removed sources and for changes of the production target
        case IncrementalityType.SBT => allSources.nonEmpty || Files.exists(target.cacheFile(project))
        case IncrementalityType.IDEA => dirty.exists(_.getFileName.toString.endsWith(ScalaCompiler.ScalaExtension))
      }
      if (run) {
        val generated = ScalaBuilder.compile(project, context, target, incrementality, dirty, allSources, sink)
        if (generated && incrementality == IncrementalityType.SBT) {
          ScalaDependencyCache.in(context).foreach(_.moduleCompiled(modules))
        }
      }
    }
  }

  private def zincCompilesJava: Boolean =
    ScalaCompilerConfiguration.instanceIn(project).incrementalityType == IncrementalityType.SBT
}

object ScalaCompiler {
  private[consulo] val ScalaExtension = ".scala"
  private[consulo] val JavaExtension = ".java"
}
