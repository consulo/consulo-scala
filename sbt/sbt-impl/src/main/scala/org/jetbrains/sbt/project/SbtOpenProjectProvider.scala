package org.jetbrains.sbt.project

import consulo.application.util.registry.Registry
import consulo.externalSystem.model.DataNode
import consulo.externalSystem.model.task.ProgressExecutionMode
import consulo.externalSystem.service.project.manage.ProjectDataManager
import consulo.externalSystem.service.project.{ExternalProjectRefreshCallback, ExternalSystemProjectRefresher, ProjectData}
import consulo.externalSystem.util.{DisposeAwareProjectChange, ExternalSystemApiUtil}
import consulo.logging.Logger
import consulo.module.content.ProjectRootManager
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import org.jetbrains.annotations.Nullable
import org.jetbrains.sbt.SbtUtil
import org.jetbrains.sbt.project.SbtOpenProjectProvider.Log
import org.jetbrains.sbt.project.settings.SbtProjectSettings

import java.util.Collections

/**
 * Links an sbt project to an existing IDE project and loads it.
 */
// upstream: AbstractBuildToolOpenProjectProvider (sbt-kotlin-utils, not ported) - opening a directory as a new project is
// done by the import provider in Consulo (SbtProjectImportProvider); the selective import dialog
// (ExternalProjectDataSelectorDialog) shown on the initial import is not ported
class SbtOpenProjectProvider {

  def isProjectFile(file: consulo.virtualFileSystem.VirtualFile): Boolean =
    SbtProjectImportProvider.canImport(file)

  def linkToExistingProject(externalProjectPath: String, project: Project): Unit =
    linkToExistingProject(externalProjectPath, project, _ => ())

  def linkToExistingProject(externalProjectPath: String, project: Project, configure: SbtProjectSettings => Unit): Unit = {
    Log.debug(s"Link SBT project '$externalProjectPath' to existing project ${project.getName}")

    val sbtProjectSettings = SbtProjectSettings.forProject(project).getOrElse(SbtProjectSettings.default)
    sbtProjectSettings.setExternalProjectPath(externalProjectPath)
    configure(sbtProjectSettings)
    attachSbtProjectAndRefresh(sbtProjectSettings, project)
  }

  private def attachSbtProjectAndRefresh(settings: SbtProjectSettings, project: Project): Unit = {
    val externalProjectPath = settings.getExternalProjectPath
    SbtUtil.sbtSettings(project).linkProject(settings)

    if (!Registry.is("external.system.auto.import.disabled", false)) {
      ExternalSystemProjectRefresher.getInstance().refreshProject(
        project,
        SbtProjectSystem.Id,
        externalProjectPath,
        new FinalImportCallback(project),
        false,
        ProgressExecutionMode.IN_BACKGROUND_ASYNC
      )
    }
  }

  private class FinalImportCallback(project: Project) extends ExternalProjectRefreshCallback {

    override def onSuccess(@Nullable externalProject: DataNode[ProjectData]): Unit = {
      if (externalProject == null) return

      ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(project) {
        @RequiredUIAccess
        override def execute(): Unit =
          ProjectRootManager.getInstance(project).mergeRootsChangesDuring { () =>
            project.getApplication.getInstance(classOf[ProjectDataManager])
              .importData(externalProject.getKey, Collections.singleton(externalProject), project, true)
          }
      })
    }

    override def onFailure(errorMessage: String, @Nullable errorDetails: String): Unit = {}
  }
}

object SbtOpenProjectProvider {
  private val Log = Logger.getInstance(classOf[SbtOpenProjectProvider])
}
