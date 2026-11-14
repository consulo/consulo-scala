package org.jetbrains.plugins.scala.compiler.consulo

import consulo.component.ProcessCanceledException
import consulo.compiler.{CompileContext, TranslatingCompiler}
import consulo.container.boot.ContainerPathManager
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.project.Project
import consulo.util.dataholder.Key
import org.jetbrains.jps.incremental.scala.shared.CompileEngine
import org.jetbrains.plugins.scala.compiler.data.{IncrementalityType, SbtData}
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.util.ScalaPluginJars

import java.nio.file.{Files, Path}
import java.util.concurrent.ConcurrentHashMap
import scala.util.control.NonFatal

/**
 * upstream: org.jetbrains.jps.incremental.scala.ScalaBuilder without the compile server: zinc always runs in the IDE
 * process (upstream LocalServer fallback), see [[CompileEngine]]
 */
private object ScalaBuilder {

  private val Log = Logger.getInstance(classOf[ScalaCompiler])

  /** zinc analysis files already reset in the current rebuild */
  private val ResetCacheFiles = Key.create[java.util.Set[Path]]("scala.compiler.reset.cache.files")

  private val PackageObjectsCleared = Key.create[java.lang.Boolean]("scala.compiler.package.objects.cleared")

  // upstream: the plugin jps directory is the directory of the compiler-jps jar, the bridges are compiled into the compile server system directory
  private lazy val sbtData: Either[String, SbtData] =
    SbtData.from(ScalaPluginJars.jpsRoot, System.getProperty("java.class.version"), ContainerPathManager.get.getSystemDir.resolve("scala-compiler"))

  /**
   * @return true if some class files were generated
   */
  def compile(project: Project,
              context: CompileContext,
              target: BuildTarget,
              incrementality: IncrementalityType,
              sources: Seq[Path],
              allSources: Seq[Path],
              sink: TranslatingCompiler.OutputSink): Boolean = {
    context.getProgressIndicator.setText(LocalizeValue.localizeTODO(s"Reading compilation settings of ${target.presentableName}"))

    val data = for {
      sbtData         <- sbtData
      compilerData    <- inReadAction(CompilerDataFactory.from(target.representativeModule, incrementality))
      compilationData <- inReadAction(CompilationDataFactory.from(project, context, target, incrementality, sources, allSources))
    } yield (sbtData, compilerData, compilationData)

    data match {
      case Left(error) =>
        context.newError(LocalizeValue.of(error)).add()
        false
      case Right((sbtData, compilerData, compilationData)) =>
        Log.info(s"Compiling ${compilationData.sources.size} files; target: ${target.presentableName}; compiler: ${compilerData.compilerJars.map(_.compilerJar).orNull}")

        if (context.isRebuild) {
          resetCacheFile(context, compilationData.cacheFile)
        }

        val client = new ConsuloClient(context, target)
        try CompileEngine.fromJpsDirectory(ScalaPluginJars.jpsRoot).compile(sbtData, compilerData, compilationData, client)
        catch {
          case e: ProcessCanceledException => throw e
          case NonFatal(e) if !client.isCanceled => client.trace(e)
          case NonFatal(_) =>
        }
        if (incrementality == IncrementalityType.IDEA && !client.hasReportedErrors && !client.isCanceled) {
          val packageObjects = packageObjectsData(project, context)
          client.packageObjectBaseSources.foreach { case (baseSource, packageObject) => packageObjects.add(baseSource, packageObject) }
          packageObjects.save()
        }
        client.registerOutputs(compilationData.output, sources, sink)
    }
  }

  /** upstream: IdeaIncrementalBuilder, the data is cleared on rebuild */
  def packageObjectsData(project: Project, context: CompileContext): PackageObjectsData = {
    val data = PackageObjectsData.getFor(project)
    if (context.isRebuild && context.getUserData(PackageObjectsCleared) == null) {
      context.putUserData(PackageObjectsCleared, java.lang.Boolean.TRUE)
      data.clear()
    }
    data
  }

  private def resetCacheFile(context: CompileContext, cacheFile: Path): Unit = {
    var reset = context.getUserData(ResetCacheFiles)
    if (reset == null) {
      reset = ConcurrentHashMap.newKeySet[Path]()
      context.putUserData(ResetCacheFiles, reset)
    }
    if (reset.add(cacheFile)) {
      Files.deleteIfExists(cacheFile)
    }
  }
}
