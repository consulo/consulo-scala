package org.jetbrains.plugins.scala.project.template

import consulo.language.editor.util.EditorHelper
import consulo.application.{ApplicationManager, ReadAction}
import consulo.ui.ModalityState
import consulo.logging.Logger
import consulo.externalSystem.importing.ImportSpecBuilder
import consulo.externalSystem.model.{ExternalSystemDataKeys, ProjectSystemId}
import consulo.externalSystem.model.task.ProgressExecutionMode
import consulo.externalSystem.service.project.ExternalSystemProjectRefresher
import consulo.externalSystem.service.project.manage.ExternalProjectsManager
import consulo.externalSystem.setting.{AbstractExternalSystemSettings, ExternalProjectSettings}
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.document.FileDocumentManager
import consulo.module.Module
import consulo.project.Project
import consulo.module.content.layer.{ContentEntry, ModifiableRootModel}
import consulo.project.startup.StartupManager
import consulo.util.dataholder.Key
import consulo.virtualFileSystem.{LocalFileSystem, VirtualFile}
import consulo.language.psi.{PsiFile, PsiManager}
import consulo.application.util.concurrent.AppExecutorUtil
import org.jetbrains.annotations.Nullable
import consulo.content.base.ExcludedContentFolderTypeProvider
import consulo.language.content.{ProductionContentFolderTypeProvider, ProductionResourceContentFolderTypeProvider, TestContentFolderTypeProvider, TestResourceContentFolderTypeProvider}
import org.jetbrains.plugins.scala.extensions.PathExt

import java.nio.file.{Files, Path}

// upstream: @ApiStatus.Internal
object ModuleBuilderUtil {
  private val Log = Logger.getInstance(this.getClass)


  def tryToSetupModule[T <: ExternalProjectSettings](
    module: Module,
    externalProjectSettings: T,
    contentEntryPath: String,
    projectSystemId: ProjectSystemId
  ): Unit = {
    val dir = getOrCreateDir(contentEntryPath)
    dir.foreach { contentRootDir =>
      doSetupModule(
        module = module,
        externalProjectSettings = externalProjectSettings,
        contentRootDir = contentRootDir,
        projectSystemId = projectSystemId
      )
    }
  }

  def doSetupModule[T <: ExternalProjectSettings](
    module: Module,
    externalProjectSettings: T,
    contentRootDir: Path,
    projectSystemId: ProjectSystemId
  ): Unit = {
    // upstream: hacks dummy ModuleData/ProjectData into ExternalSystemModulePropertyManager so that external system
    // realizes it can remove this module after sbt import (ModuleDataService.computeOrphanData),
    // Consulo keeps external system module options in the module extension

    val project = module.getProject
    val settings =
      ExternalSystemApiUtil.getSettings(project, projectSystemId)
        .asInstanceOf[AbstractExternalSystemSettings[?, T, ?]]

    externalProjectSettings.setExternalProjectPath(contentRootDir.toCanonicalPath.toString)
    settings.linkProject(externalProjectSettings)

    FileDocumentManager.getInstance.saveAllDocuments()
    val runExternalSystemProjectRefreshOnProjectOpen = shouldRunExternalSystemProjectRefreshOnProjectOpen(module)

    StartupManager.getInstance(project).runAfterOpened { () =>
      /** similar code is also called inside [[com.intellij.openapi.externalSystem.service.ExternalSystemStartupActivity.runActivity]]
       * In case the refresh below is not finished yet another refresh is cancelled in
       * `com.intellij.openapi.externalSystem.util.ExternalSystemUtil`*/
      // upstream: ExternalProjectsManagerImpl.setStoreExternally(false) + init(), Consulo has no external storage configuration
      ExternalProjectsManager.getInstance(project).runWhenInitialized { () =>
        if (runExternalSystemProjectRefreshOnProjectOpen) {
          ExternalSystemProjectRefresher.getInstance.refreshProjects(
            new ImportSpecBuilder(project, projectSystemId)
              .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
          )
        } else {
          // Project import is skiped in those NPW tests that only test the generated files, without imporintgthe full project
          Log.assertTrue(ApplicationManager.getApplication.isUnitTestMode, "Project import cant be skipped in tests only")
          // upstream: also resets ExternalSystemDataKeys.NEWLY_CREATED_PROJECT
          project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, null)
        }
      }
    }
  }


  private val RunExternalSystemProjectRefreshOnProjectOpenKey: Key[java.lang.Boolean] =
    Key.create("scala.module.builder.run.external.system.project.refresh.on.project.open")

  private def shouldRunExternalSystemProjectRefreshOnProjectOpen(module: Module): Boolean = {
    val valueOpt = Option(module.getUserData(RunExternalSystemProjectRefreshOnProjectOpenKey))
    valueOpt.forall(_.booleanValue())
  }

  def setRunExternalSystemProjectRefreshOnProjectOpen(module: Module, value: Boolean): Unit = {
    module.putUserData(RunExternalSystemProjectRefreshOnProjectOpenKey, value: java.lang.Boolean)
  }

  @deprecated
  def tryToSetupRootModel(
    model: ModifiableRootModel,
    @Nullable contentEntryPath: String,
    contentEntryFolders: Option[DefaultModuleContentEntryFolders] = None
  ): Unit = {
    Option(contentEntryPath).foreach(tryToSetupRootModel2(model, _, contentEntryFolders))
  }

  def tryToSetupRootModel2(
    model: ModifiableRootModel,
    contentEntryPath: String,
    contentEntryFolders: Option[DefaultModuleContentEntryFolders] = None
  ): Unit = {
    for {
      contentRootDir <- getOrCreateDir(contentEntryPath)
      vFile <- Option(LocalFileSystem.getInstance.refreshAndFindFileByNioFile(contentRootDir))
    } {
      doSetupRootModel(model, vFile, contentEntryFolders)
    }
  }

  /**
   * A three-step non-blocking read action sequence for opening files after project creation.
   *
   *   1. An action is registered to run after the new project is opened.
   *      After a behavioural change in the platform in 252, we are no longer allowed to interact with
   *      the PSI in any way inside this action.
   *      It seems that this causes a freeze in the UI.
   *   1. The first action kicks-off a non-blocking read action to query all `PsiFile` instances
   *      which correspond to the provided `VirtualFile` instances as an argument.
   *      The run action part runs on a background thread but scheduled as a separate computation.
   *      That way, the PSI access is outside the first action.
   *   1. Finally, the non-blocking read action finishes by scheduling a computation on the UI thread.
   *      Here, the queried `PsiFile` instances are opened in editors.
   */
  def openFilesInEditor(files: Seq[VirtualFile], project: Project): Unit = {
    if (files.isEmpty) return
    //noinspection ApiStatus
    StartupManager.getInstance(project).runAfterOpened(() => openFiles(files, project))
  }

  private def openFiles(files: Seq[VirtualFile], project: Project): Unit = {
    if (project.isDisposed) return

    ReadAction
      .nonBlocking[Array[PsiFile]](() => {
        val manager = PsiManager.getInstance(project)
        files.flatMap(vf => Option(manager.findFile(vf))).toArray
      })
      .expireWhen(() => project.isDisposed)
      .finishOnUiThread(_ => ModalityState.nonModal(), EditorHelper.openFilesInEditor(_))
      .submit(AppExecutorUtil.getAppExecutorService)
  }

  private def doSetupRootModel(
    model: ModifiableRootModel,
    vContentRootDir: VirtualFile,
    contentEntryFolders: Option[DefaultModuleContentEntryFolders]
  ): Unit = {
    val entry: ContentEntry = model.addContentEntry(vContentRootDir)
    // upstream: model.inheritSdk(), in Consulo the SDK belongs to the module extension

    contentEntryFolders.foreach(markDefaultModelContentEntryFolders(entry, vContentRootDir, _))
  }

  private def markDefaultModelContentEntryFolders(
    entry: ContentEntry,
    vContentRootDir: VirtualFile,
    folders: DefaultModuleContentEntryFolders,
  ): Unit = {
    def url(relativePath: String): String =
      vContentRootDir.toString + java.io.File.separator + relativePath

    folders.sources.map(url).foreach(entry.addFolder(_, ProductionContentFolderTypeProvider.getInstance))
    folders.testSources.map(url).foreach(entry.addFolder(_, TestContentFolderTypeProvider.getInstance))
    folders.resources.map(url).foreach(entry.addFolder(_, ProductionResourceContentFolderTypeProvider.getInstance))
    folders.testResources.map(url).foreach(entry.addFolder(_, TestResourceContentFolderTypeProvider.getInstance))
    folders.excluded.map(url).foreach(entry.addFolder(_, ExcludedContentFolderTypeProvider.getInstance))
  }

  private def getOrCreateDir(dirPath: String): Option[Path] =
    if (dirPath.nonEmpty) {
      val dir = Files.createDirectories(Path.of(dirPath))
      Option(dir).filter(_.exists)
    } else None
}
