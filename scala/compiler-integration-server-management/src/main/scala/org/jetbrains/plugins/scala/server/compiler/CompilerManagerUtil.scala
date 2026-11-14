package org.jetbrains.plugins.scala.server.compiler
import consulo.compiler.CompilerPaths
import consulo.project.Project

import java.nio.file.{Files, Path}

object CompilerManagerUtil {

  /**
   * A [[java.nio.file.Path]] implementation of
   * [[com.intellij.openapi.compiler.CompilerManager#getJavacCompilerWorkingDir()]].
   */
  // upstream: BuildManager.getProjectSystemDir(project), the JPS project system directory
  def javacCompilerWorkingDir(project: Project): Path = {
    val projectBuildDir = CompilerPaths.getCompilerSystemDirectory(project).toPath
    Files.createDirectories(projectBuildDir)
    projectBuildDir
  }
}
