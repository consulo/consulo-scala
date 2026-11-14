package org.jetbrains.plugins.scala.compiler.consulo

import com.intellij.java.compiler.impl.javaCompiler.javac.{JavacCompilerConfiguration, JavacSettingsBuilder}
import consulo.compiler.{CompileContext, CompileContextEx, CompilerEncodingService, CompilerPaths}
import consulo.module.{Module, ModuleManager}
import consulo.project.Project
import consulo.util.collection.Chunk
import org.jetbrains.plugins.scala.compiler.data.{CompilationData, IncrementalityType, ZincData}
import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettings

import java.io.IOException
import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

/**
 * upstream: org.jetbrains.jps.incremental.scala.data.CompilationDataFactory (JPS model -> Consulo project model)
 */
private object CompilationDataFactory {

  def from(project: Project,
           context: CompileContext,
           target: BuildTarget,
           incrementality: IncrementalityType,
           sources: Seq[Path],
           allSources: Seq[Path]): Either[String, CompilationData] = {
    val module = target.representativeModule

    for {
      output <- target.outputDir(context).toRight(s"Output directory not specified for module ${module.getName}")
      _ <- outputClash(context, target, output).toLeft(())
    } yield {
      checkOrCreate(output)

      val classpath = target.classpath
      val compilerSettings = ScalaCompilerSettings.forModule(module)
      val preferredEncoding = target.modules.iterator
        .flatMap(m => Option(CompilerEncodingService.getInstance(project).getPreferredModuleEncoding(m)))
        .nextOption()
        .map(_.name())

      def ensureEncodingIsExplicitlySet(compilerOptions: Seq[String]): Seq[String] =
        compilerOptions.find {
          case "-encoding" | "--encoding" => true
          case s"-encoding:${_}" | s"--encoding:${_}" => true
          case _ => false
        } match {
          case Some(_) => compilerOptions
          case None => preferredEncoding.toSeq.flatMap(Seq("-encoding", _)) ++ compilerOptions
        }

      val scalaOptions = compilerSettings.getOptionsAsStrings(module.hasScala3)
      val javaOptions = javaOptionsFor(project, target)

      val classpathSet = classpath.toSet
      val outputToCacheMap = createOutputToCacheMap(project).filter { case (dir, _) => dir != output && classpathSet.contains(dir) }

      val outputGroups = target.sourceRoots.filter(Files.exists(_)).map(_ -> output)

      val compiledSources = incrementality match {
        // upstream: the IDEA incrementality type passes the java sources only with the Mixed compile order, scalac only parses them
        case IncrementalityType.IDEA => (sources.filter(_.getFileName.toString.endsWith(ScalaCompiler.ScalaExtension)) ++
          allSources.filter(_.getFileName.toString.endsWith(ScalaCompiler.JavaExtension))).distinct
        case IncrementalityType.SBT => sources
      }

      // upstream: !JavaBuilderUtil.isCompileJavaIncrementally && !JavaBuilderUtil.isForcedRecompilationAllJavaModules
      val isCompile = !context.isMake && !context.isRebuild

      CompilationData(
        sources = compiledSources,
        classpath = classpath,
        output = output,
        scalaOptions = filterOutPipeliningOptions(ensureEncodingIsExplicitlySet(scalaOptions)),
        javaOptions = ensureEncodingIsExplicitlySet(javaOptions),
        order = compilerSettings.compileOrder,
        cacheFile = target.cacheFile(project),
        outputToCacheMap = outputToCacheMap,
        outputGroups = outputGroups,
        zincData = ZincData(
          allSources = if (incrementality == IncrementalityType.SBT) allSources else Seq.empty,
          compilationStartDate = context.asInstanceOf[CompileContextEx].getStartCompilationStamp,
          isCompile = isCompile
        )
      )
    }
  }

  private def filterOutPipeliningOptions(compilerOptions: Seq[String]): Seq[String] = {
    val withoutBooleans = compilerOptions.to(mutable.ArrayBuffer).filter {
      case "-Xjava-tasty" | "-Xpickle-java" | "-Yjava-tasty" | "-Ypickle-java" => false
      case "-Xallow-outline-from-tasty" | "-Yallow-outline-from-tasty" => false
      case "-Ypickle-write-api-only" => false
      case _ => true
    }

    def remove(option: String): Unit = {
      var index = -1
      while ({
        index = withoutBooleans.indexOf(option)
        index != -1
      }) {
        withoutBooleans.remove(index, 2)
      }
    }

    List("-Xearly-tasty-output", "-Xpickle-write", "-Yearly-tasty-output", "-Ypickle-write").foreach(remove)
    withoutBooleans.toSeq
  }

  /**
   * upstream: CompilerDataFactory.javaOptionsFor (JavacOptionsProvider + JavaBuilder.addCompilationOptions),
   * here the options of the consulo-java javac settings and of the module java extension
   */
  private def javaOptionsFor(project: Project, target: BuildTarget): Seq[String] = {
    val chunk = new Chunk[Module](target.modules.toSet.asJava)
    val settings = new JavacSettingsBuilder(JavacCompilerConfiguration.getInstance(project)).getOptions(chunk).asScala.toSeq
    val extensionOptions = CompilerDataFactory.javaExtension(target.representativeModule).toSeq.flatMap { extension =>
      Option(extension.getBytecodeVersion).toSeq.flatMap(Seq("--release", _)) ++ extension.getCompilerArguments.asScala
    }
    settings ++ extensionOptions
  }

  private def checkOrCreate(output: Path): Unit = {
    if (!Files.exists(output)) {
      try Files.createDirectories(output)
      catch {
        case t: Throwable => throw new IOException("Cannot create output directory: " + output.toString, t)
      }
    }
  }

  private def outputClash(context: CompileContext, target: BuildTarget, output: Path): Option[String] = {
    val other = target.copy(isTests = !target.isTests)
    if (other.outputDir(context).contains(output) && other.sourceRoots.nonEmpty && target.sourceRoots.nonEmpty)
      Some(s"Output path $output is shared between production and test sources of ${target.representativeModule.getName}\n" +
        "Configure separate output paths to proceed with the compilation.")
    else None
  }

  /**
   * zinc analysis files of the other targets, used to look up the classes of their output directories
   */
  private def createOutputToCacheMap(project: Project): Map[Path, Path] =
    (for {
      module <- ModuleManager.getInstance(project).getModules.toSeq if module.hasScala
      isTests <- Seq(false, true)
      output <- Option(CompilerPaths.getModuleOutputDirectory(module, isTests))
    } yield output.toAbsolutePath.normalize() -> BuildTarget.cacheFile(project, module, isTests)).toMap
}
