package org.jetbrains.plugins.scala.bazel

import consulo.annotation.component.ExtensionImpl
import consulo.bazel.workspace.BazelModelBuilder
import org.jetbrains.bazel.scala.sdk.{ScalaSdk, ScalaSdkExtension}
import org.jetbrains.plugins.scala.project.external.ScalaSdkUtils
import org.jetbrains.plugins.scala.project.{ReplClasspath, ScalaLibraryProperties, ScalaLibraryType}

import java.nio.file.Path
import scala.jdk.CollectionConverters.*

/**
 * Makes the project library which the Scala modules of a Bazel project depend on (named after the Scala version) a
 * Scala SDK: the compiler classpath is the Scala SDK jars of the Bazel Scala toolchain.
 */
@ExtensionImpl
final class BazelScalaSdkExtension extends ScalaSdkExtension {
  override def addScalaSdk(scalaSdk: ScalaSdk, modelsProvider: BazelModelBuilder): Unit = {
    val library = modelsProvider.getOrCreateLibrary(scalaSdk.name)
    val model = modelsProvider.getLibraryModifiableModel(library)
    val compilerClasspath = scalaSdk.sdkJars.asScala.toSeq.map(Path.of(_))
    model.setKind(ScalaLibraryType.Kind)
    //NOTE: must be called after `setKind` because it resets the properties
    model.setProperties(ScalaLibraryProperties(
      version = Option(scalaSdk.scalaVersion),
      compilerClasspath = compilerClasspath,
      scaladocExtraClasspath = Nil,
      compilerBridgeBinaryJar = ScalaSdkUtils.resolveCompilerBridgeJar(modelsProvider.getProject, scalaSdk.scalaVersion),
      replClasspath = ReplClasspath.Bundled
    ))
  }
}
