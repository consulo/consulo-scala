package org.jetbrains.plugins.scala.project.external

import consulo.module.Module
import consulo.project.Project
import consulo.content.library.{Library, LibraryTable}
import consulo.module.content.layer.ModifiableRootModel
import consulo.platform.Platform
import org.jetbrains.plugins.scala.DependencyManagerBase.RichStr
import org.jetbrains.plugins.scala.EelAwareDependencyManager
import org.jetbrains.plugins.scala.project.{LibraryBase, LibraryExt, ReplClasspath, ScalaLibraryProperties, ScalaLibraryType}
import org.jetbrains.plugins.scala.rt.project.Version

import java.nio.file.Path

//noinspection ApiStatus,UnstableApiUsage
object ScalaSdkUtils {

  def configureScalaSdk(
    module: Module,
    compilerVersion: String,
    scalacClasspath: Seq[Path],
    scaladocExtraClasspath: Seq[Path],
    compilerBridgeBinaryJar: Option[Path],
    replClasspath: ReplClasspath,
    sdkPrefix: String,
    projectLibrariesModel: LibraryTable.ModifiableModel,
    rootModel: ModifiableRootModel
  ): Unit = {
    val scalaSDKLibraryName = scalaSdkLibraryName(sdkPrefix, compilerVersion)
    doConfigureScalaSdk(
      libraries = projectLibrariesModel.getLibraries.toSeq,
      isApplicable = (library: Library) => isApplicableScalaSdk(library, scalaSDKLibraryName),
      createLibrary = projectLibrariesModel.createLibrary(scalaSDKLibraryName),
      ensureConvertedToScalaSdk = (library: Library) => ScalaSdkUtils.ensureScalaLibraryIsConvertedToScalaSdk(
        library,
        scalacClasspath,
        scaladocExtraClasspath,
        compilerBridgeBinaryJar,
        replClasspath
      ),
      addToModule = (library: Library) => rootModel.addLibraryEntry(library)
    )
  }

  private def isApplicableScalaSdk(library: LibraryBase, scalaSDKLibraryName: String): Boolean =
    library.isScalaSdk && library.name.orNull == scalaSDKLibraryName

  private def scalaSdkLibraryName(sdkPrefix: String, compilerVersion: String): String =
    s"$sdkPrefix: scala-sdk-$compilerVersion"

  private def doConfigureScalaSdk[T] (
    libraries: => Seq[T],
    isApplicable: T => Boolean,
    createLibrary: => T,
    ensureConvertedToScalaSdk: T => Unit,
    addToModule: T => Unit
  ): Unit = {
    val existingScalaSDKForSpecificVersion = libraries.find(isApplicable)
    val scalaSdkLibrary = existingScalaSDKForSpecificVersion.getOrElse(createLibrary)

    ensureConvertedToScalaSdk(scalaSdkLibrary)
    addToModule(scalaSdkLibrary)
  }

  private def ensureScalaLibraryIsConvertedToScalaSdk(
    library: Library,
    compilerClasspath: Seq[Path],
    scaladocExtraClasspath: Seq[Path],
    compilerBridgeBinaryJar: Option[Path],
    replClasspath: ReplClasspath
  ): Unit = {
    val modifiableModel = library.getModifiableModel
    doEnsureScalaLibraryIsConvertedToScalaSdk(
      library,
      modifiableModel.setKind(ScalaLibraryType.Kind),
      properties => modifiableModel.setProperties(properties),
      compilerClasspath,
      scaladocExtraClasspath,
      compilerBridgeBinaryJar,
      replClasspath
    )
    modifiableModel.commit()
  }

  private def doEnsureScalaLibraryIsConvertedToScalaSdk(
    library: LibraryBase,
    setScalaSdkKind: => Unit,
    setProperties: ScalaLibraryProperties => Unit,
    compilerClasspath: Seq[Path],
    scaladocExtraClasspath: Seq[Path],
    compilerBridgeBinaryJar: Option[Path],
    replClasspath: ReplClasspath
  ): Unit = {
    val properties = ScalaLibraryProperties(library.libraryVersion, compilerClasspath, scaladocExtraClasspath, compilerBridgeBinaryJar, replClasspath)
    if (!library.isScalaSdk) {
      setScalaSdkKind
    }
    //NOTE: must be called after `setKind` because later resets the properties
    setProperties(properties)
  }

  @deprecated("Use resolveCompilerBridgeJar(Platform, String) instead", since = "2026.1")
  @Deprecated(since = "2026.1", forRemoval = true)
  // upstream: @ApiStatus.ScheduledForRemoval(inVersion = "2026.2")
  def resolveCompilerBridgeJar(scalaVersion: String): Option[Path] =
    resolveCompilerBridgeJar(Platform.current(), scalaVersion)

  def resolveCompilerBridgeJar(project: Project, scalaVersion: String): Option[Path] =
    resolveCompilerBridgeJar(project.getPlatform, scalaVersion)

  def resolveCompilerBridgeJar(platform: Platform, scalaVersion: String): Option[Path] =
    compilerBridgeName(scalaVersion)
      .map(name => "org.scala-lang" % name % scalaVersion)
      .map(dep => new EelAwareDependencyManager().resolveSafeAndTransferToRemoteEel(platform, dep))
      .flatMap(_.headOption)

  @deprecated("Use resolveReplClasspath(Platform, ScalaVersion) instead", since = "2026.1")
  @Deprecated(since = "2026.1", forRemoval = true)
  // upstream: @ApiStatus.ScheduledForRemoval(inVersion = "2026.2")
  def resolveReplClasspath(scalaVersion: String): ReplClasspath =
    resolveReplClasspath(Platform.current(), scalaVersion)

  def resolveReplClasspath(project: Project, scalaVersion: String): ReplClasspath =
    resolveReplClasspath(project.getPlatform, scalaVersion)

  def resolveReplClasspath(platform: Platform, scalaVersion: String): ReplClasspath = {
    val version = Version(scalaVersion)
    if (version.major(2) < Version("3.8")) return ReplClasspath.Bundled
    val dep = ("org.scala-lang" % "scala3-repl_3" % scalaVersion).transitive()
    val paths = new EelAwareDependencyManager().resolveSafeAndTransferToRemoteEel(platform, dep)
    ReplClasspath.Provided(paths)
  }

  def compilerBridgeName(scalaVersion: String): Option[String] = {
    val version = Version(scalaVersion)
    if (version.major(1) == Version("2")) {
      if (version >= Version("2.13.12")) {
        // Scala 2.13.12 and later versions distribute their own precompiled compiler bridge with support for
        // compiler diagnostics.
        Some(Scala2CompilerBridgeName)
      } else
        None // Previous Scala 2 versions should use the bundled source based Zinc compiler bridge
    } else Some(Scala3CompilerBridgeName)
  }

  def compilerBridgeJarName(scalaVersion: String): Option[String] =
    compilerBridgeName(scalaVersion).map(n => s"$n-$scalaVersion.jar")

  /**
   * Revert the Scala SDK kind from all existing Scala libraries that shouldn't currently be SDKs.
   * For a detailed explanation of why this method is needed, see `org.jetbrains.sbt.project.data.service.SbtProjectDataService#revertScalaSdkFromLibraries`.
   */
  def revertScalaSdkFromLibraries(
    projectLibrariesModel: LibraryTable.ModifiableModel,
    rootModels: Seq[ModifiableRootModel],
    externalSystemName: String
  ): Unit = {
    // upstream: library.getExternalSource.getDisplayName == externalSystemName, Consulo libraries have no external source,
    // external system libraries are prefixed with the system name (see ExternalSystemApiUtil.isExternalSystemLibrary)
    def isFromExternalSource(library: Library): Boolean =
      Option(library.getName).exists(_.startsWith(externalSystemName + ": "))

    val projectLibraries = projectLibrariesModel.getLibraries.toSeq
    val moduleLibraries = rootModels.flatMap(_.getModuleLibraryTable.getLibraries.toSeq)
    val scalaRuntimeLibraries = (projectLibraries ++ moduleLibraries).filter(_.hasRuntimeLibrary)

    scalaRuntimeLibraries
      .filter { library => library.isScalaSdk && isFromExternalSource(library) && !library.getName.contains("scala-sdk") }
      .foreach { library =>
        val model = library.getModifiableModel
        model.setKind(null)
        model.commit()
      }
  }

  private final val Scala3CompilerBridgeName = "scala3-sbt-bridge"

  private final val Scala2CompilerBridgeName = "scala2-sbt-bridge"
}
