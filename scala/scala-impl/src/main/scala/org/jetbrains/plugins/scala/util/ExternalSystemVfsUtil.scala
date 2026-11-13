package org.jetbrains.plugins.scala.util

import consulo.compiler.util.CompilerUtil
import consulo.compiler.CompilerPaths
import consulo.externalSystem.model.ProjectSystemId
import consulo.module.ModuleManager
import consulo.module.content.ModuleRootManager
import consulo.virtualFileSystem.util.VirtualFileUtil
import consulo.component.ProcessCanceledException
import consulo.application.progress.ProgressIndicator
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.project.ModuleExt

import java.nio.file.Path
import scala.jdk.CollectionConverters._

object ExternalSystemVfsUtil {

  /**
   * Refreshes the source content roots and output paths of all modules in a project. Used by sbt shell and BSP after
   * building the project. As both sbt shell and BSP run the project build in an external process, the IDEA Virtual
   * File System needs to be manually notified which file system directories have new content and need to be refreshed.
   *
   * @note Common code used by `SbtProjectTaskRunnerImpl` and `BspProjectTaskRunner`. The purpose of this utility
   *       function is to avoid bugs by forgetting to update the code in two places. Apart from that, it has limited
   *       functionality.
   */
  // re-evaluate this utility function when the platform changes how run configuration classpaths are constructed, IDEA-343184
  // ProcessCanceledException might be thrown com.intellij.compiler.impl.CompilerUtil.refreshOutputRoots if the indicator is canceled
  @throws[ProcessCanceledException]
  def refreshRoots(project: Project, id: ProjectSystemId, indicator: ProgressIndicator): Unit = {
    indicator.setText(ScalaBundle.message("refresh.roots.synchronizing.output.directories"))
    try {
      // simply refresh all the source roots to catch any generated files -- this MAY have a performance impact
      // in which case it might be necessary to receive the generated sources directly from sbt and refresh them (see BuildManager)
      // upstream: the roots were taken from the external project data (ProjectDataManager.getExternalProjectData), which
      // Consulo doesn't keep, so the source roots of the modules are used
      val allModules = ModuleManager.getInstance(project).getModules.filterNot(_.hasBuildModuleType)
      val generatedSourceRoots = allModules.toSeq
        .flatMap(module => ModuleRootManager.getInstance(module).getSourceRootUrls(true))
        .map(VirtualFileUtil.urlToPath)
        .distinct

      // Because we don't have an exact way of knowing which modules have been affected, we need to refresh the output
      // directories of all modules in the project. Otherwise, we run the risk that the Run Configuration order
      // enumerator will not see all output directories in the VFS and will not put them on the runtime classpath.
      // In Gradle, affected modules are collected using an injected Gradle script, which tracks which modules are
      // affected by a build command.
      // https://github.com/JetBrains/intellij-community/blob/bf3083ca66771e038eb1c64128b4e508f52acfad/plugins/gradle/java/src/execution/build/GradleProjectTaskRunner.java#L60
      val outputRoots = CompilerPaths.getOutputPaths(allModules)

      val toRefresh = generatedSourceRoots ++ outputRoots
      val toRefreshFiles = toRefresh.map(Path.of(_)).asJava
      // upstream: CompilerUtil.refreshOutputRoots
      CompilerUtil.refreshDirectories(toRefreshFiles)

      // This is most likely not necessary. Gradle only invokes `CompilerUtil.refreshOutputRoots`.
      // Recursively refreshing the output directories is comparatively expensive. It is enough for the Run Configuration
      // order enumerator to just refresh the output directories without their children, but we don't have tests in place
      // in order to be more confident in this change.
      // https://github.com/JetBrains/intellij-community/blob/bf3083ca66771e038eb1c64128b4e508f52acfad/plugins/gradle/java/src/execution/build/GradleProjectTaskRunner.java#L174-L176
      // upstream: LocalFileSystem.refreshNioFiles(toRefreshFiles, true, true, null)
      CompilerUtil.refreshFiles(toRefreshFiles)
    } finally {
      //noinspection ScalaExtractStringToBundle
      indicator.setText("")
    }
  }
}
