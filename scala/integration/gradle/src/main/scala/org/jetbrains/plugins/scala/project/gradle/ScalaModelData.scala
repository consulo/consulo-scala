package org.jetbrains.plugins.scala.project.gradle

import consulo.externalSystem.model.{Key, ProjectKeys}
import consulo.externalSystem.service.project.AbstractExternalEntityData
import consulo.gradle.GradleConstants

import java.io.File
import java.util.Objects

/**
 * upstream: org.jetbrains.plugins.gradle.model.data.ScalaModelData of the IntelliJ Gradle plugin. Consulo Gradle has no such data:
 * [[ScalaGradleProjectResolverExtension]] computes everything needed for the Scala SDK in the background resolve phase.
 *
 * @param scalaVersion            null if it cannot be detected from the compiler classpath
 * @param compilerBridgeBinaryJar null if the bundled compiler bridge is used
 * @param replClasspath           empty if the REPL is bundled with the compiler
 */
final class ScalaModelData(
  val scalaVersion: String,
  val scalaClasspath: java.util.List[File],
  val compilerBridgeBinaryJar: File,
  val replClasspath: java.util.List[File],
  val compilerOptions: java.util.List[String]
) extends AbstractExternalEntityData(GradleConstants.SYSTEM_ID) {

  /**
   * Only invoked by the External System serialization mechanism.
   */
  def this() = this(null, java.util.Collections.emptyList(), null, java.util.Collections.emptyList(), java.util.Collections.emptyList())

  override def equals(obj: Any): Boolean = obj match {
    case that: ScalaModelData =>
      scalaVersion == that.scalaVersion &&
        scalaClasspath == that.scalaClasspath &&
        compilerBridgeBinaryJar == that.compilerBridgeBinaryJar &&
        replClasspath == that.replClasspath &&
        compilerOptions == that.compilerOptions
    case _ => false
  }

  override def hashCode(): Int = Objects.hash(scalaVersion, scalaClasspath, compilerBridgeBinaryJar, replClasspath, compilerOptions)
}

object ScalaModelData {
  val Key: Key[ScalaModelData] = new Key(classOf[ScalaModelData].getName, ProjectKeys.LIBRARY_DEPENDENCY.getProcessingWeight + 1)
}
