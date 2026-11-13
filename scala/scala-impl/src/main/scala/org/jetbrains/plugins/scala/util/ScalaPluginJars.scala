package org.jetbrains.plugins.scala.util

import consulo.util.io.FileUtil
import consulo.util.nodep.io.FileUtilRt
import consulo.util.io.ClassPathUtil.getJarPathForClass
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import org.jetbrains.plugins.scala.extensions.PathExt

import java.nio.file.Path
import scala.util.parsing.combinator.RegexParsers
import scala.xml.Node

object ScalaPluginJars {

  val libRoot: Path = {
    val jarPath = Path.of(getJarPathForClass(this.getClass)) // scalaCommunity.jar
    jarPath.getParent
  }

  val jpsRoot: Path = libRoot / "jps"

  val scalaLibraryJar: Path = libRoot / "scala-library.jar"
  val compileServerScalaLibraryJar: Path = jpsRoot / "compile-server-scala-library.jar"
  val compileServerScala3LibraryJar: Path = jpsRoot / "compile-server-scala3-library_3.jar"
  val scalaReflectJar: Path = libRoot / "scala-reflect.jar"
  val scalaNailgunRunnerJar: Path = libRoot / "scala-nailgun-runner.jar"
  val compilerSharedJar: Path = libRoot / "compiler-shared.jar"
  val scalaJpsJar: Path = libRoot / "scala-jps.jar"
  val runnersJarName: String = "runners.jar"
  val runnersJar: Path = libRoot / runnersJarName
  val replInterface: Path = libRoot / "repl-interface.jar"
  val worksheetReplInterfaceImplsJar: Path = libRoot.getParent / "worksheet-repl-interface" / "impls.jar"
  val semanticDecompiler: Path = libRoot / "semantic" / "decompiler.jar"
  val utilsRt: Path = libRoot / "utils_rt.jar"

  val nailgunJar: Path = jpsRoot / "nailgun.jar"
  val compilerInterfaceJar: Path = jpsRoot / "compiler-interface.jar"
  val sbtInterfaceJar: Path = jpsRoot / "sbt-interface.jar"
  val incrementalCompilerJar: Path = jpsRoot / "incremental-compiler.jar"
  val compileServerJar: Path = jpsRoot / "compile-server.jar"
  val compilerJpsJar: Path = jpsRoot / "compiler-jps.jar"
  val compilerPluginJar_2_12: Path = jpsRoot / "compiler-plugin-2.12.jar"
  val compilerPluginJar_2_13: Path = jpsRoot / "compiler-plugin-2.13.jar"
  val compilerPluginJar_3_3: Path = jpsRoot / "compiler-plugin-3.3.jar"
}

object CompilerBridgeSourcesJars {
  import ScalaPluginJars.jpsRoot

  private def sources(version: String): String = s"compiler-bridge-sources_$version.jar"

  private val compilerBridgeSources_2_10: Path = jpsRoot / sources("2.10")
  private val compilerBridgeSources_2_11: Path = jpsRoot / sources("2.11")
  private val compilerBridgeSources_2_12: Path = jpsRoot / sources("2.12")
  private val compilerBridgeSources_2_13: Path = jpsRoot / sources("2.13")

  val allBridgeSources: Seq[Path] = Seq(
    compilerBridgeSources_2_10,
    compilerBridgeSources_2_11,
    compilerBridgeSources_2_12,
    compilerBridgeSources_2_13
  )
}

object IntellijPlatformJars {
  // upstream: also jpsBuildersJar, asmJar (org.jetbrains.org.objectweb.asm) and protobufJava - JPS build process jars, no JPS in Consulo
  val utilJar: Path = Path.of(getJarPathForClass(classOf[FileUtil]))
  val utilRtJar: Path = Path.of(getJarPathForClass(classOf[FileUtilRt]))
  val fastUtilJar: Path = Path.of(getJarPathForClass(classOf[Int2ObjectMap[?]]))
}

object LibraryJars {
  val scalaParserCombinators: Path = Path.of(getJarPathForClass(classOf[RegexParsers]))
  val scalaXml: Path = Path.of(getJarPathForClass(classOf[Node]))
}
