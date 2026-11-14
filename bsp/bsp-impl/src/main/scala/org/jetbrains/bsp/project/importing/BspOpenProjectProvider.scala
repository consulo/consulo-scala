package org.jetbrains.bsp.project.importing

import consulo.externalSystem.model.DataNode
import consulo.externalSystem.model.task.ProgressExecutionMode
import consulo.externalSystem.service.project.manage.ProjectDataManager
import consulo.externalSystem.service.project.{ExternalProjectRefreshCallback, ExternalSystemProjectRefresher, ProjectData}
import consulo.externalSystem.util.{DisposeAwareProjectChange, ExternalSystemApiUtil}
import consulo.module.content.ProjectRootManager
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import org.jetbrains.annotations.Nullable
import org.jetbrains.bsp.{BSP, BspUtil}
import org.jetbrains.bsp.settings.BspProjectSettings

import java.util.Collections

/**
 * Links a BSP workspace to an existing IDE project and loads it.
 */
// upstream: AbstractBuildToolOpenProjectProvider (sbt-kotlin-utils, not ported) - opening a directory as a new project is
// done by the import provider in Consulo (BspProjectImportProvider); the BSP connection file generation before the refresh
// (generateBspConfig) and the selective import dialog (ExternalProjectDataSelectorDialog) are not part of the linking
class BspOpenProjectProvider {

  def linkToExistingProject(externalProjectPath: String, project: Project): Unit = {
    val bspProjectSettings = new BspProjectSettings()
    bspProjectSettings.setExternalProjectPath(externalProjectPath)
    linkToExistingProject(bspProjectSettings, project)
  }

  def linkToExistingProject(settings: BspProjectSettings, project: Project): Unit = {
    val externalProjectPath = settings.getExternalProjectPath
    BspUtil.bspSettings(project).linkProject(settings)

    ExternalSystemProjectRefresher.getInstance().refreshProject(
      project,
      BSP.ProjectSystemId,
      externalProjectPath,
      new BspOpenProjectProvider.FinalImportCallback(project),
      false,
      ProgressExecutionMode.IN_BACKGROUND_ASYNC
    )
  }
}

private object BspOpenProjectProvider {

  // TODO duplicated with org.jetbrains.sbt.project.SbtOpenProjectProvider.FinalImportCallback
  private class FinalImportCallback(project: Project) extends ExternalProjectRefreshCallback {

    override def onSuccess(@Nullable externalProject: DataNode[ProjectData]): Unit = {
      if (externalProject == null || project.isDisposed) return

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
