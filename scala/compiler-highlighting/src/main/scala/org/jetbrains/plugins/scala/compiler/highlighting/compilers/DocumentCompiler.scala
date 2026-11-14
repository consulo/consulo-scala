package org.jetbrains.plugins.scala.compiler.highlighting.compilers

import consulo.annotation.component.{ComponentScope, ServiceAPI, ServiceImpl}
import consulo.compiler.CompilerPaths
import consulo.container.boot.ContainerPathManager
import consulo.document.Document
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.util.ModuleUtilCore
import consulo.module.Module
import consulo.module.content.layer.OrderEnumerator
import consulo.project.Project
import consulo.util.io.FileUtil
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.{Inject, Singleton}
import org.jetbrains.jps.incremental.scala.shared.remote.{SerializablePath, SourceScope}
import org.jetbrains.jps.incremental.scala.shared.{Client, CompileEngine, DelegateClient}
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.api.project.ScalaLanguageLevel
import org.jetbrains.plugins.scala.compiler.data.*
import org.jetbrains.plugins.scala.compiler.highlighting.core.CompilerOptions
import org.jetbrains.plugins.scala.editor.DocumentExt
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettings
import org.jetbrains.plugins.scala.project.{ModuleExt, VirtualFileExt}
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.util.ScalaPluginJars

import java.nio.file.{Files, Path}

/**
 * upstream: sends the document to the Scala compile server (RemoteServerRunner), here it is compiled in the IDE
 * process by the in-process compile engine ([[CompileEngine.compile]], see scala/compile-server LocalServer).
 * The document is written to a temporary file in the working directory, as upstream does by default (the in-memory
 * mode of the registry key `scala.compiler.highlighting.document.use.in.memory.file` is not ported).
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
final class DocumentCompiler @Inject()(project: Project) {

  // upstream: <javac compiler working directory>/document-compiler
  private def workingDirectory(): Path = {
    val compilerDir = ContainerPathManager.get.getSystemDir
      .resolve("scala-document-compiler")
      .resolve(project.getName.replaceAll("[^\\w.-]", "_") + "." + project.getLocationHash)
    if (!compilerDir.exists) {
      Files.createDirectories(compilerDir)
    }
    compilerDir
  }

  def compile(
    module: Module,
    sourceScope: SourceScope,
    document: Document,
    virtualFile: VirtualFile,
    client: Client
  ): Unit = {
    val originalSourceFile = virtualFile.toPath
    val sourceContent = document.textWithConvertedSeparators(virtualFile)
    compilePhysicalFile(originalSourceFile, sourceContent, module, sourceScope, client)
  }

  private def compilePhysicalFile(originalSourceFile: Path,
                                  content: String,
                                  module: Module,
                                  sourceScope: SourceScope,
                                  client: Client): Unit = {
    val sourceName = originalSourceFile.getFileName.toString
    val workingDir = workingDirectory()
    val tempSourceFile = workingDir.resolve(sourceName)
    val connector =
      try {
        Files.writeString(tempSourceFile, content)
        new PhysicalFileConnector(tempSourceFile, workingDir, module, sourceScope)
      } catch {
        case t: Throwable =>
          // Remove the temporary source file if creating the connector failed.
          FileUtil.delete(tempSourceFile)
          throw t
      }

    try connector.compile(originalSourceFile, client)
    finally {
      if (connector.requiresCleanup) {
        cleanWorkingDirectory(workingDir)
      } else {
        FileUtil.delete(tempSourceFile)
      }
    }
  }

  private def cleanWorkingDirectory(workingDir: Path): Unit = {
    val files = workingDir.children()
    files.foreach(FileUtil.delete)
  }

  /**
   * upstream: PhysicalFileConnector + AbstractRemoteServerConnector (RemoteServerConnectorBase of
   * compiler-integration), the arguments are built like for the worksheet compilation
   * (worksheet RemoteServerConnector)
   */
  private final class PhysicalFileConnector(tempSourceFile: Path, outputDir: Path, module: Module, sourceScope: SourceScope) {

    var requiresCleanup: Boolean = false

    def compile(originalSourceFile: Path, client: Client): Unit = {
      val fixedClient = new DelegateClient(client) {
        override def message(msg: Client.ClientMsg): Unit = {
          /**
           * NOTE: some compiler errors can be with empty `source`<br>
           * Example: `bad option '-g:vars' was ignored`<br>
           * We do not want to loose such message.
           * We rely that they will be reported in the beginning of the file<br>
           * see [[org.jetbrains.plugins.scala.compiler.highlighting.services.core.HighlightInfoFactory]]
           * (we assume that `from` and `to` are also empty for such files)
           */
          val fixedSource = Some(SerializablePath(originalSourceFile, pathTranslator))
          val fixedMsg = msg.copy(source = fixedSource)
          client.message(fixedMsg)
        }

        override def compilationEnd(sources: Set[Path]): Unit = {
          val fixedSources = Set(originalSourceFile)
          client.compilationEnd(fixedSources)
        }
      }
      val compilationData = this.compilationData
      DocumentCompiler.engine.compile(DocumentCompiler.sbtData, compilerData, compilationData, fixedClient)
    }

    private def compilerSettings: ScalaCompilerSettings = ScalaCompilerSettings.forModule(module)

    private def compilerData: CompilerData =
      CompilerData(
        compilerJars = CompilerJarsFactory.fromFiles(module.scalaCompilerClasspath, module.customScalaCompilerBridgeJar, module.replClasspath.asPaths).toOption,
        javaHome = javaHome,
        incrementalType = IncrementalityType.IDEA
      )

    private def compilationData: CompilationData =
      CompilationData(
        sources = Seq(tempSourceFile),
        classpath = runtimeClasspath,
        output = outputDir,
        scalaOptions = scalaParameters,
        javaOptions = Seq.empty,
        order = CompileOrder.valueOf(compilerSettings.compileOrder.name),
        cacheFile = Path.of(""),
        outputToCacheMap = Map.empty,
        outputGroups = Seq(outputDir -> outputDir),
        zincData = ZincData(
          allSources = Seq.empty,
          compilationStartDate = 0,
          isCompile = false
        )
      )

    private def scalaParameters: Seq[String] = {
      var scalacOptions = CompilerOptions.scalacOptions(module)
      // The setting is per-project rather than per-module
      if (ScalaProjectSettings.getInstance(project).isUseCompilerTypes) {
        DocumentCompiler.compilerPluginJar(module).foreach { jar =>
          scalacOptions :++= Seq("-Xplugin:" + jar.toString, "-Xplugin-require:intellij-compiler-plugin")
          if (module.scalaLanguageLevel.contains(ScalaLanguageLevel.Scala_2_12)) {
            scalacOptions :+= "-Yrangepos"
          }
        }
      }
      if (!CompilerOptions.containsStopAfter(scalacOptions)) {
        val stopAfter = module.scalaLanguageLevel match {
          case Some(ScalaLanguageLevel.Scala_2_10) => Some("-Ystop-after:dce")
          case Some(ScalaLanguageLevel.Scala_2_11) |
               Some(ScalaLanguageLevel.Scala_2_12) |
               Some(ScalaLanguageLevel.Scala_2_13) => Some("-Ystop-after:delambdafy")
          case Some(languageLevel) if languageLevel.isScala3 => Some("-Ystop-after:repeatableAnnotations")
          case _ =>
            // .class files will be produced, they should be cleaned up
            requiresCleanup = true
            None
        }
        scalacOptions ++= stopAfter
      } else {
        // .class/tasty files might be produced, they should be cleaned up if they exist
        requiresCleanup = true
      }
      if (module.scalaLanguageLevel.exists(_ >= ScalaLanguageLevel.Scala_3_3)) {
        if (!CompilerOptions.containsUnusedImports(scalacOptions)) {
          scalacOptions = scalacOptions :+ "-Wunused:imports"
        }
      }
      scalacOptions
    }

    // upstream: RemoteServerConnectorBase.runtimeClasspath + the module output of the source scope, without the
    // compiler classpath and the runners, compiler-shared and scala-jps jars of the plugin
    private def runtimeClasspath: Seq[Path] = {
      val enumerator = OrderEnumerator.orderEntries(module).compileOnly().recursively()
      val classesRoots = enumerator.getClassesRoots.toSeq.map(_.toPath).map(stripJarPathSuffix).map(Path.of(_))
      val forTestClasses = sourceScope match {
        case SourceScope.Production => false
        case SourceScope.Test => true
      }
      val outputDirs = (Seq(false) ++ Option.when(forTestClasses)(true))
        .flatMap(forTests => Option(CompilerPaths.getModuleOutputPath(module, forTests)))
        .map(Path.of(_))
      (classesRoots ++ outputDirs).distinct :+ outputDir
    }

    private def stripJarPathSuffix(path: Path): String =
      path.toCanonicalPath.toString.stripSuffix("!").stripSuffix("!/")

    // upstream: the JDK of the compile server
    private def javaHome: Option[Path] =
      Option(ModuleUtilCore.getSdk(module, classOf[JavaModuleExtension[?]]))
        .flatMap(sdk => Option(sdk.getHomePath))
        .map(Path.of(_))
  }
}

object DocumentCompiler {

  private def engine: CompileEngine = CompileEngine.fromJpsDirectory(ScalaPluginJars.jpsRoot)

  // upstream: the compile server system directory
  private lazy val sbtData: SbtData =
    SbtData.from(ScalaPluginJars.jpsRoot, System.getProperty("java.class.version"), ContainerPathManager.get.getSystemDir.resolve("scala-compiler"))
      .fold(message => throw new IllegalArgumentException(message), identity)

  /**
   * The compiler plugin which reports the types of the expressions ("Use types reported by the Scala compiler").
   * upstream: packaged into lib/jps by the compiler-plugin modules, which are not ported (no jar - no compiler types)
   */
  def compilerPluginJar(module: Module): Option[Path] = {
    val jar = module.scalaLanguageLevel.flatMap {
      case ScalaLanguageLevel.Scala_2_12 if module.scalaMinorVersion.exists(_ >= ScalaVersion.fromString("2.12.13").get) => Some(ScalaPluginJars.compilerPluginJar_2_12)
      case ScalaLanguageLevel.Scala_2_13 if module.scalaMinorVersion.exists(_ >= ScalaVersion.fromString("2.13.1").get) => Some(ScalaPluginJars.compilerPluginJar_2_13)
      case level if level >= ScalaLanguageLevel.Scala_3_3 => Some(ScalaPluginJars.compilerPluginJar_3_3)
      case _ => None
    }
    jar.filter(Files.exists(_))
  }

  def get(project: Project): DocumentCompiler =
    project.getInstance(classOf[DocumentCompiler])
}
