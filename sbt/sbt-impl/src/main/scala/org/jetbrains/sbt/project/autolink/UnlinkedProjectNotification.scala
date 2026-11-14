package org.jetbrains.sbt.project.autolink

import consulo.application.{Application, WriteAction}
import consulo.localize.LocalizeValue
import consulo.module.ModuleManager
import consulo.module.creation.ModuleCreationHelper
import consulo.module.creation.importing.{ModuleImportContext, ModuleImportProvider}
import consulo.project.ui.notification.{NotificationGroup, NotificationType}
import consulo.project.{Project, ProjectPropertiesComponent}
import consulo.ui.ex.action.{AnAction, AnActionEvent}
import consulo.util.lang.Pair

import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.CompletableFuture

/**
 * The "load project" notification of an unlinked build found in the project base directory, shared by sbt and BSP.
 *
 * upstream: ExternalSystemUnlinkedProjectAware - the platform shows the notification and links the project. Consulo has no
 * unlinked project detection; like consulo-gradle a startup activity offers to import the build with the import provider of
 * the build system (the import chooser shows its wizard steps).
 */
object UnlinkedProjectNotification {

  def isEnabled(project: Project, dontShowAgainKey: String): Boolean =
    ProjectPropertiesComponent.getInstance(project).getBoolean(dontShowAgainKey, true)

  def show(
    project: Project,
    group: NotificationGroup,
    title: LocalizeValue,
    content: LocalizeValue,
    loadActionText: LocalizeValue,
    dontShowAgainKey: String,
    importProvider: Class[? <: ModuleImportProvider[?]],
    pathToImport: Path
  ): Unit = {
    val notification = group.createNotification(title.get, content.get, NotificationType.INFORMATION, null)
    notification.addAction(new AnAction(loadActionText) {
      override def actionPerformed(e: AnActionEvent): Unit = {
        notification.expire()
        importProject(project, importProvider, pathToImport)
      }
    })
    notification.addAction(new AnAction(LocalizeValue.localizeTODO("Don't show again")) {
      override def actionPerformed(e: AnActionEvent): Unit = {
        notification.expire()
        ProjectPropertiesComponent.getInstance(project).setValue(dontShowAgainKey, false, true)
      }
    })
    notification.notify(project)
  }

  private def importProject(project: Project, importProviderClass: Class[? <: ModuleImportProvider[?]], pathToImport: Path): Unit = {
    val provider: ModuleImportProvider[?] = Application.get().getExtensionPoint(classOf[ModuleImportProvider[?]]).findExtensionOrFail(importProviderClass)
    val result = new CompletableFuture[Pair[ModuleImportContext, ModuleImportProvider[ModuleImportContext]]]()
    Application.get().getInstance(classOf[ModuleCreationHelper])
      .showImportChooser(project, pathToImport, Collections.singletonList[ModuleImportProvider[?]](provider), result)

    result.whenComplete { (pair, error) =>
      if (error == null && pair != null) {
        val modifiableModel = ModuleManager.getInstance(project).getModifiableModel
        pair.getSecond.process(pair.getFirst, project, modifiableModel, _ => ())
        WriteAction.runAndWait(() => modifiableModel.commit())
      }
    }
  }
}
