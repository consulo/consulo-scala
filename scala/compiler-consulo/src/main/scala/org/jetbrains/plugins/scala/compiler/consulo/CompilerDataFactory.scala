package org.jetbrains.plugins.scala.compiler.consulo

import consulo.content.library.Library
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.module.Module
import consulo.module.content.ModuleRootManager
import org.jetbrains.plugins.scala.compiler.data.CompilerJarsFactory.CompilerJarsResolveError
import org.jetbrains.plugins.scala.compiler.data.{CompilerData, CompilerJars, CompilerJarsFactory, IncrementalityType}
import org.jetbrains.plugins.scala.project.{LibraryExt, ModuleExt}
import org.jetbrains.plugins.scala.shared.util.JarUtil
import org.jetbrains.plugins.scala.shared.util.JarUtil.JarFileWithName

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * upstream: org.jetbrains.jps.incremental.scala.data.CompilerDataFactory (JPS model -> Consulo project model)
 */
private object CompilerDataFactory {

  def from(module: Module, incrementality: IncrementalityType): Either[String, CompilerData] = {
    val compilerJars: Either[String, Option[CompilerJars]] =
      module.scalaSdk
        .map(extractCompilerJars(_, module).map(Some(_)))
        .getOrElse(Right(None))

    for {
      jars <- compilerJars
      home <- javaHome(module)
    } yield CompilerData(jars, home, incrementality)
  }

  private def extractCompilerJars(scalaSdk: Library, module: Module): Either[String, CompilerJars] = {
    val properties = scalaSdk.properties
    CompilerJarsFactory.fromFiles(properties.compilerClasspath, properties.compilerBridgeBinaryJar, properties.replClasspath.asPaths)
      .flatMap(validateAllFilesExist)
      .left.map(toErrorMessage(_, scalaSdk, module))
  }

  private def validateAllFilesExist(jars: CompilerJars): Either[CompilerJarsResolveError.FilesDoNotExist, CompilerJars] = {
    val absentJars = jars.allJars.filterNot(Files.exists(_))
    Either.cond(absentJars.isEmpty, jars, CompilerJarsResolveError.FilesDoNotExist(absentJars))
  }

  // upstream: None when the module JDK is the JDK of the compile server; here javac is always forked from the module JDK
  private def javaHome(module: Module): Either[String, Option[Path]] =
    javaExtension(module)
      .flatMap(extension => Option(extension.getSdk))
      .flatMap(sdk => Option(sdk.getHomePath))
      .toRight("No JDK in module " + module.getName)
      .flatMap { homePath =>
        val directory = Path.of(homePath)
        Either.cond(Files.exists(directory), Some(directory), "JDK home directory does not exists: " + directory)
      }

  def javaExtension(module: Module): Option[JavaModuleExtension[?]] =
    ModuleRootManager.getInstance(module).getExtensions.asScala.collectFirst { case e: JavaModuleExtension[?] => e }

  private def toErrorMessage(error: CompilerJarsResolveError, scalaSdk: Library, module: Module): String = {
    import CompilerJarsResolveError.*

    def inScalaCompiler = s"in Scala compiler classpath in Scala SDK ${scalaSdk.getName}"
    def filesNames(files: Iterable[JarFileWithName]) = files.map(_.name).mkString(", ")
    def filePaths(absentJars: Iterable[Path]) = absentJars.map(_.toAbsolutePath.normalize().toString).mkString(", ")

    import JarUtil.JarExtension

    error match {
      case NotFound(kind)               => s"No '$kind*$JarExtension' $inScalaCompiler"
      case DuplicatesFound(kind, files) => s"Multiple '$kind*$JarExtension' files (${filesNames(files)}) $inScalaCompiler"
      case FilesDoNotExist(absentJars)  => s"Scala compiler JARs not found (module '${module.getName}'): ${filePaths(absentJars)}"
    }
  }
}
