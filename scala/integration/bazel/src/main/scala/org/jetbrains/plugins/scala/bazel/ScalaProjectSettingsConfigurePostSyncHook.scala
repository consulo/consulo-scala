package org.jetbrains.plugins.scala.bazel

import consulo.annotation.component.ExtensionImpl
import consulo.project.Project
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

import java.util.concurrent.CompletableFuture

@ExtensionImpl
final class ScalaProjectSettingsConfigurePostSyncHook extends ProjectPostSyncHook {

  override def isEnabled(project: Project): Boolean = org.jetbrains.bazel.config.project.isBazelProject(project)

  override def onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment): CompletableFuture[Void] = {
    val project = environment.project
    // compiler highlighting does not work with Bazel projects as the build is not automatically run each sync
    val scalaProjectSettings = ScalaProjectSettings.getInstance(project)
    scalaProjectSettings.setCompilerHighlightingScala2(false)
    scalaProjectSettings.setCompilerHighlightingScala3(false)
    // Bazel projects have issues when compiling with Zinc, hence this enforcement.
    // SCL-23923 Customers' project failed to locally build with Zinc
    ScalaCompilerConfiguration.instanceIn(project).incrementalityType = IncrementalityType.IDEA
    CompletableFuture.completedFuture(null)
  }
}
