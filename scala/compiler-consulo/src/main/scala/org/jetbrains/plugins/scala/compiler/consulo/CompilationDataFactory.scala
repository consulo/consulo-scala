package org.jetbrains.plugins.scala.compiler.consulo

import com.intellij.java.compiler.impl.javaCompiler.JavaCompilerConfiguration
import com.intellij.java.compiler.impl.javaCompiler.javac.{JavacCompilerConfiguration, JavacSettingsBuilder}
import com.intellij.java.language.impl.projectRoots.JavaSdkVersionUtil
import com.intellij.java.language.projectRoots.JavaSdkVersion
import consulo.java.compiler.impl.javaCompiler.JavaAdditionalOutputDirectoriesProvider
import consulo.util.io.FileUtil
import consulo.compiler.{CompileContext, CompileContextEx, CompilerEncodingService, CompilerPaths}
import consulo.module.{Module, ModuleManager}
import consulo.project.Project
import consulo.util.collection.Chunk
import org.jetbrains.plugins.scala.compiler.data.{CompilationData, IncrementalityType, ZincData}
import org.jetbrains.plugins.scala.extensions.inReadAction
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

      val configuredScalaOptions = compilerSettings.getOptionsAsStrings(module.hasScala3)
      val scalaOptions = semanticDbOptionsFor(configuredScalaOptions, project) ++ configuredScalaOptions
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

  private val SourceRootOptionScala2 = "-P:semanticdb:sourceroot"
  private val SourceRootOptionScala3 = "-sourceroot"
  private val SemanticDbOptionScala3 = "-Xsemanticdb"

  /**
   * When semantic db is enabled (via compiler plugin in Scala 2 or via compiler option in Scala3)
   * the source root is set to the project base directory, unless it is configured explicitly
   */
  private def semanticDbOptionsFor(configuredOptions: Seq[String], project: Project): Seq[String] = {
    val isSemanticDbEnabledScala2 = configuredOptions.exists(isSemanticDbPluginOption)
    val hasSemanticDbPluginSourceRootOptionScala2 = configuredOptions.exists(_.startsWith(SourceRootOptionScala2))

    val isSemanticDbEnabledScala3 = configuredOptions.contains(SemanticDbOptionScala3)
    val hasSemanticDbPluginSourceRootOptionScala3 = configuredOptions.contains(SourceRootOptionScala3)

    val baseDirectory = Option(project.getBasePath).map(Path.of(_).toAbsolutePath.toString)

    if (isSemanticDbEnabledScala2 && !hasSemanticDbPluginSourceRootOptionScala2)
      baseDirectory.map(dir => s"$SourceRootOptionScala2:$dir").toSeq
    else if (isSemanticDbEnabledScala3 && !hasSemanticDbPluginSourceRootOptionScala3)
      baseDirectory.map(dir => s"$SourceRootOptionScala3:$dir").toSeq
    else
      Seq.empty
  }

  private def isSemanticDbPluginOption(compilerOption: String): Boolean = {
    val xPluginPrefix = "-Xplugin:"
    compilerOption.startsWith(xPluginPrefix) &&
      compilerOption.contains("semanticdb-scalac") &&
      Files.exists(Path.of(compilerOption.substring(xPluginPrefix.length)))
  }

  /**
   * upstream: CompilerDataFactory.javaOptionsFor (JavacOptionsProvider + JavaBuilder.addCompilationOptions),
   * here the options of the consulo-java javac settings, of the module java extension and of the annotation processing profile
   */
  private def javaOptionsFor(project: Project, target: BuildTarget): Seq[String] = {
    val chunk = new Chunk[Module](target.modules.toSet.asJava)
    val settings = new JavacSettingsBuilder(JavacCompilerConfiguration.getInstance(project)).getOptions(chunk).asScala.toSeq
    val extensionOptions = CompilerDataFactory.javaExtension(target.representativeModule).toSeq.flatMap { extension =>
      Option(extension.getBytecodeVersion).toSeq.flatMap(Seq("--release", _)) ++ extension.getCompilerArguments.asScala
    }
    val processingOptions = annotationProcessingOptions(project, target.representativeModule)
    settings.filterNot(_.startsWith("-proc:")) ++ extensionOptions ++ processingOptions
  }

  /**
   * upstream: JavaBuilder.addCompilationOptions with the annotation processing profile of the module.
   * Generated sources go to the directory of consulo-java's annotation processing compiler (production directory only).
   */
  private def annotationProcessingOptions(project: Project, module: Module): Seq[String] = inReadAction {
    val profile = JavaCompilerConfiguration.getInstance(project).getAnnotationProcessingConfiguration(module)
    if (profile == null || !profile.isEnabled) Seq("-proc:none")
    else {
      val generatedSources = Option(JavaAdditionalOutputDirectoriesProvider.getAnnotationProcessorsGenerationPath(module)).toSeq.flatMap { dir =>
        Files.createDirectories(Path.of(dir))
        Seq("-s", Path.of(dir).toString)
      }
      val processorPath =
        if (profile.isObtainProcessorsFromClasspath) Seq.empty
        else Seq("-processorpath", FileUtil.toSystemDependentName(profile.getProcessorPath))
      val processors = profile.getProcessors.asScala.toSeq match {
        case Seq() => Seq.empty
        case names => Seq("-processor", names.mkString(","))
      }
      val processorOptions = profile.getProcessorOptions.asScala.toSeq.map { case (key, value) => s"-A$key=$value" }
      // for javac from version 23 annotation processing must be enabled explicitly
      val procFull = CompilerDataFactory.javaExtension(module)
        .flatMap(extension => Option(JavaSdkVersionUtil.getJavaSdkVersion(extension.getSdk)))
        .filter(_.isAtLeast(JavaSdkVersion.JDK_23))
        .map(_ => "-proc:full")
      processorPath ++ processors ++ processorOptions ++ procFull ++ generatedSources
    }
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
