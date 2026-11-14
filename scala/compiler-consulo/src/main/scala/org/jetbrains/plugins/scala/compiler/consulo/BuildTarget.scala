package org.jetbrains.plugins.scala.compiler.consulo

import consulo.compiler.{CompileContext, CompilerManager, CompilerPaths}
import consulo.language.content.LanguageContentFolderScopes
import consulo.module.Module
import consulo.module.content.ModuleRootManager
import consulo.module.content.layer.OrderEnumerator
import consulo.project.Project
import consulo.util.io.URLUtil
import consulo.virtualFileSystem.util.VirtualFileUtil

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Using

/**
 * Consulo-specific: the production or test part of a module chunk, compiled into one output directory with one zinc
 * analysis file. upstream: the JPS `ModuleBuildTarget`s of a `ModuleChunk`.
 */
private final case class BuildTarget(modules: Seq[Module], isTests: Boolean) {

  def representativeModule: Module = modules.head

  def presentableName: String =
    modules.map(_.getName).mkString(", ") + (if (isTests) " (tests)" else "")

  def outputDir(context: CompileContext): Option[Path] =
    Option(if (isTests) context.getModuleOutputDirectoryForTests(representativeModule) else context.getModuleOutputDirectory(representativeModule))
      .map(_.toAbsolutePath.normalize())

  def cacheFile(project: Project): Path =
    BuildTarget.cacheFile(project, representativeModule, isTests)

  def sourceRoots: Seq[Path] = modules.flatMap { module =>
    val scope = if (isTests) LanguageContentFolderScopes.onlyTest() else LanguageContentFolderScopes.onlyProduction()
    ModuleRootManager.getInstance(module).getContentFolderFiles(scope).toSeq.map(_.toNioPath.toAbsolutePath.normalize())
  }.distinct

  private def excludedRoots: Seq[Path] = modules.flatMap { module =>
    ModuleRootManager.getInstance(module).getContentFolderFiles(LanguageContentFolderScopes.excluded()).toSeq.map(_.toNioPath.toAbsolutePath.normalize())
  }

  /**
   * All .scala and .java files of the target (upstream: SbtBuilder.collectCompilableFiles)
   */
  def allSources(context: CompileContext): Seq[Path] = {
    val compilerManager = CompilerManager.getInstance(context.getProject)
    val excluded = excludedRoots
    sourceRoots.filter(Files.isDirectory(_)).flatMap { root =>
      Using.resource(Files.walk(root)) { stream =>
        stream.iterator.asScala
          .filter { file =>
            val name = file.getFileName.toString
            (name.endsWith(ScalaCompiler.ScalaExtension) || name.endsWith(ScalaCompiler.JavaExtension)) &&
              Files.isRegularFile(file) &&
              !excluded.exists(file.startsWith) &&
              !compilerManager.isExcludedFromCompilation(file)
          }
          .map(_.toAbsolutePath.normalize())
          .toSeq
      }
    }.distinct
  }

  /**
   * upstream: ProjectPaths.getCompilationClasspathFiles(chunk, includeTests, excludeMainModuleOutput = false, exportedOnly = true)
   */
  def classpath: Seq[Path] = modules.flatMap { module =>
    val enumerator = OrderEnumerator.orderEntries(module).compileOnly().withoutSdk().recursively().exportedOnly()
    val urls = (if (isTests) enumerator else enumerator.productionOnly()).classes().getUrls
    urls.toSeq.map { url =>
      val path = VirtualFileUtil.urlToPath(url)
      val separator = path.indexOf(URLUtil.ARCHIVE_SEPARATOR)
      Path.of(if (separator >= 0) path.substring(0, separator) else path)
    }
  }.distinct
}

private object BuildTarget {

  def cacheFile(project: Project, module: Module, isTests: Boolean): Path = {
    val name = module.getName.replaceAll("[^\\w.-]", "_")
    CompilerPaths.getCompilerSystemDirectory(project).toPath
      .resolve("scala")
      .resolve(s"cache-$name-${if (isTests) "test" else "production"}.zip")
  }
}
