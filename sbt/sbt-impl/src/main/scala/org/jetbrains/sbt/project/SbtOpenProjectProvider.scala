package org.jetbrains.sbt.project

import consulo.disposer.Disposable
import consulo.application.ApplicationManager
import consulo.logging.Logger
import consulo.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import consulo.externalSystem.service.project.ProjectData
import consulo.externalSystem.model.{DataNode, ProjectSystemId}
import consulo.externalSystem.model.task.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import consulo.externalSystem.service.project.manage.ProjectDataManager
import com.intellij.openapi.externalSystem.service.ui.ExternalProjectDataSelectorDialog
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import consulo.project.Project
import consulo.disposer.Disposer
import consulo.application.util.registry.Registry
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.sbt.SbtUtil
import org.jetbrains.sbt.project.SbtOpenProjectProvider.Log
import org.jetbrains.sbt.project.settings.SbtProjectSettings
import org.jetbrains.sbt.settings.SbtSettings

//noinspection UnstableApiUsage
class SbtOpenProjectProvider extends AbstractBuildToolOpenProjectProvider {

  override def getSystemId: ProjectSystemId = SbtProjectSystem.Id

  override def isProjectFile(file: VirtualFile): Boolean =
    SbtProjectImportProvider.canImport(file)

  override protected def doLinkProject(projectDirectory: VirtualFile, project: Project): Unit = {
    Log.debug(s"Link SBT project '$projectDirectory' to existing project ${project.getName}")

    val sbtProjectSettings = SbtProjectSettings.forProject(project).getOrElse(SbtProjectSettings.default)
    sbtProjectSettings.setExternalProjectPath(projectDirectory.toNioPath.toString)
    attachSbtProjectAndRefresh(sbtProjectSettings, project)
  }

  private def attachSbtProjectAndRefresh(settings: SbtProjectSettings, project: Project): Unit = {
    val externalProjectPath = settings.getExternalProjectPath
    SbtUtil.sbtSettings(project).linkProject(settings)

    if (!Registry.is("external.system.auto.import.disabled")) {
      ExternalSystemUtil.refreshProject(
        externalProjectPath,
        new ImportSpecBuilder(project, SbtProjectSystem.Id)
          .usePreviewMode()
          .use(ProgressExecutionMode.MODAL_SYNC)
      )
      ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized { () =>
        ExternalSystemUtil.refreshProject(
          externalProjectPath,
          new ImportSpecBuilder(project, SbtProjectSystem.Id)
            .withCallback(new FinalImportCallback(project, settings))
            .withImportProjectData(false)
        )
      }
    }
  }


  // TODO duplicated with org.jetbrains.bsp.project.BspOpenProjectProvider.FinalImportCallback
  private class FinalImportCallback(project: Project, projectSettings: SbtProjectSettings)
    extends ExternalProjectRefreshCallback {

    override def onSuccess(externalProject: DataNode[ProjectData]): Unit = {

      if (externalProject == null) return

      def selectDataTask = {
        val projectInfo =
          new InternalExternalProjectInfo(SbtProjectSystem.Id, projectSettings.getExternalProjectPath, externalProject)
        val dialog = new ExternalProjectDataSelectorDialog(project, projectInfo)
        if (dialog.hasMultipleDataToSelect)
          dialog.showAndGet()
        else
          Disposer.dispose(dialog.getDisposable: Disposable)
      }

      def importTask(): Unit = {
        ProjectDataManager.getInstance().importData(externalProject, project)
      }

      val showSelectiveImportDialog = SbtSettings.getInstance(project).showSelectiveImportDialogOnInitialImport()
      val application = ApplicationManager.getApplication

      if (showSelectiveImportDialog && !application.isHeadlessEnvironment) {
        application.invokeLater { () =>
          selectDataTask
          application.executeOnPooledThread { (() => importTask()): Runnable }
        }
      }
      else {
        importTask()
      }
    }
  }
}

object SbtOpenProjectProvider {
  private val Log = Logger.getInstance(classOf[SbtOpenProjectProvider])
}
