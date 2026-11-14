package org.jetbrains.plugins.scala.compiler.consulo

import consulo.annotation.component.ExtensionImpl
import consulo.java.compiler.JavaCompilerSourceFilter
import consulo.module.Module
import consulo.project.Project
import jakarta.inject.Inject
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration

import java.nio.file.Path

/** Consulo only: with sbt incrementality zinc compiles the Java files of Scala modules ([[ScalaCompiler]]), not javac. */
@ExtensionImpl
final class ScalaJavaCompilerSourceFilter @Inject()(project: Project) extends JavaCompilerSourceFilter {
  override def isCompilableByJavaCompiler(module: Module, file: Path): Boolean =
    ScalaCompilerConfiguration.instanceIn(project).incrementalityType != IncrementalityType.SBT || !inReadAction(module.hasScala)
}
