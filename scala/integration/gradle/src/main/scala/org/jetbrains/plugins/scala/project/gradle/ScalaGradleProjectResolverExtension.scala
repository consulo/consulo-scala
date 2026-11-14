package org.jetbrains.plugins.scala.project.gradle

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.DataNode
import consulo.externalSystem.model.project.ModuleData
import consulo.gradle.service.project.AbstractProjectResolverExtension
import consulo.platform.Platform
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.tooling.model.ProjectImportAction
import org.jetbrains.plugins.gradle.tooling.scala.{ScalaCompileOptions, ScalaModel}
import org.jetbrains.plugins.scala.project.LibraryExt
import org.jetbrains.plugins.scala.project.external.{ReplClasspathCachedResolver, ScalaSdkUtils}

import java.io.File
import java.util
import java.util.Collections
import scala.jdk.CollectionConverters.*

// upstream: @Order(ExternalSystemConstants.UNORDERED)
@ExtensionImpl
final class ScalaGradleProjectResolverExtension extends AbstractProjectResolverExtension {

  //noinspection ApiStatus
  private lazy val replClasspathResolver = ReplClasspathCachedResolver(Platform.current())

  override def populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode[ModuleData]): Unit = {
    val models = resolverCtx.getModels.asInstanceOf[ProjectImportAction.AllModels]
    Option(models.getExtraProject(gradleModule, classOf[ScalaModel])).foreach { scalaModel =>
      ideModule.createChild(ScalaModelData.Key, dataOf(scalaModel))
    }
    nextResolver.populateModuleExtraModels(gradleModule, ideModule)
  }

  override def getExtraProjectModelClasses: util.Set[Class[?]] = Collections.singleton(classOf[ScalaModel])

  // upstream: the Gradle model is copied to ScalaModelData, ScalaGradleDataService computes the compiler options and
  // resolves the compiler bridge and the REPL classpath
  private def dataOf(model: ScalaModel): ScalaModelData = {
    // Gradle's ScalaModel#getScalaClasspath only exposes java.io.File; there is no nio.Path-based alternative.
    //noinspection SSBasedInspection
    val classpath = Option(model.getScalaClasspath).map(_.asScala.toSeq).getOrElse(Seq.empty)
    // Gradle's ScalaModel#getScalaCompilerPlugins only exposes java.io.File; there is no nio.Path-based alternative.
    //noinspection SSBasedInspection
    val compilerPlugins = Option(model.getScalaCompilerPlugins).map(_.asScala.toSeq).getOrElse(Seq.empty)
    val compilerOptions = Option(model.getScalaCompileOptions).toSeq.flatMap(compilerOptionsFrom(_, compilerPlugins))
    val scalaVersion = compilerVersionOf(classpath)

    // Only resolve the compiler bridge for Scala 3. Gradle reports a compiler classpath that doesn't work with
    // the Scala 2.13.12+ compiler bridges, due to clashes.
    val compilerBridgeBinaryJar = scalaVersion.filter(_.startsWith("3.")).flatMap { version =>
      ScalaSdkUtils.compilerBridgeJarName(version).flatMap { bridgeJarName =>
        classpath.find(_.getName == bridgeJarName)
          .orElse(ScalaSdkUtils.resolveCompilerBridgeJar(Platform.current(), version).map(_.toFile))
      }
    }

    val replClasspath = scalaVersion.toSeq.flatMap(replClasspathResolver.resolve(_).asPaths.map(_.toFile))

    new ScalaModelData(
      scalaVersion.orNull,
      new util.ArrayList(classpath.asJava),
      compilerBridgeBinaryJar.orNull,
      new util.ArrayList(replClasspath.asJava),
      new util.ArrayList(compilerOptions.asJava)
    )
  }

  private def compilerVersionOf(classpath: Seq[File]): Option[String] = {
    import LibraryExt.*
    val scalaLibrariesInCompilerClasspath = classpath.map(_.getName).filter(isRuntimeLibrary)
    scalaLibrariesInCompilerClasspath.flatMap(runtimeVersion).headOption
  }

  private def compilerOptionsFrom(options: ScalaCompileOptions, compilerPlugins: Seq[File]): Seq[String] = {
    val presentations = Seq(
      options.isDeprecation -> "-deprecation",
      options.isUnchecked -> "-unchecked",
      options.isOptimize -> "-optimise",
      !isEmpty(options.getDebugLevel) -> s"-g:${options.getDebugLevel}",
      !isEmpty(options.getEncoding) -> s"-encoding",
      // the encoding value needs to be a separate option, otherwise the -encoding flag and the value will be
      // treated as a single flag
      !isEmpty(options.getEncoding) -> options.getEncoding
    )

    val scalaCompilerPlugins = compilerPlugins.map(f => s"-Xplugin:${f.getPath}")

    val additionalOptions =
      if (options.getAdditionalParameters != null) options.getAdditionalParameters.asScala else Seq.empty

    presentations.flatMap(include.tupled) ++ scalaCompilerPlugins ++ additionalOptions
  }

  private def isEmpty(s: String) = s == null || s.isEmpty

  private def include(b: Boolean, s: String): Seq[String] = if (b) Seq(s) else Seq.empty
}
