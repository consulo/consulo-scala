package org.jetbrains.sbt.project.autolink

import consulo.annotation.component.ExtensionImpl
import consulo.application.dumb.DumbAware
import consulo.application.{Application, WriteAction}
import consulo.externalSystem.model.ExternalSystemDataKeys
import consulo.module.ModuleManager
import consulo.module.creation.ModuleCreationHelper
import consulo.module.creation.importing.{ModuleImportContext, ModuleImportProvider}
import consulo.project.startup.PostStartupActivity
import consulo.project.ui.notification.NotificationType
import consulo.project.{Project, ProjectPropertiesComponent}
import consulo.ui.UIAccess
import consulo.ui.ex.action.{AnAction, AnActionEvent}
import consulo.localize.LocalizeValue
import consulo.util.lang.Pair
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.project.SbtProjectImportProvider
import org.jetbrains.sbt.settings.SbtSettings

import java.nio.file.{Files, Path}
import java.util.Collections
import java.util.concurrent.CompletableFuture

// upstream: SbtUnlinkedProjectAware (ExternalSystemUnlinkedProjectAware + SbtTextProvider). Consulo has no unlinked project
// detection, so like consulo-gradle a startup activity offers to import a build.sbt found in the project base directory.
@ExtensionImpl
final class SbtUnlinkedProjectStartupActivity extends PostStartupActivity with DumbAware {

  import SbtUnlinkedProjectStartupActivity.*

  override def runActivity(project: Project, uiAccess: UIAccess): Unit = {
    val basePath = project.getBasePath
    if (basePath == null ||
      !ProjectPropertiesComponent.getInstance(project).getBoolean(ShowUnlinkedProjectNotification, true) ||
      !SbtSettings.getInstance(project).getLinkedProjectsSettings.isEmpty ||
      project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) == java.lang.Boolean.TRUE)
      return

    val buildFile = Path.of(basePath).resolve(Sbt.BuildFile)
    if (!Files.isRegularFile(buildFile) || SbtUnlinkedProjectAwareHelper.isLinkedProject(project, basePath))
      return

    val notification = ScalaNotificationGroups.sbtProjectImport.createNotification(
      LocalizeValue.localizeTODO("sbt build script found").get,
      LocalizeValue.localizeTODO("The project directory contains a build.sbt file which is not imported").get,
      NotificationType.INFORMATION,
      null
    )
    notification.addAction(new AnAction(LocalizeValue.localizeTODO("Load sbt Project")) {
      override def actionPerformed(e: AnActionEvent): Unit = {
        notification.expire()
        importProject(project, buildFile)
      }
    })
    notification.addAction(new AnAction(LocalizeValue.localizeTODO("Don't show again")) {
      override def actionPerformed(e: AnActionEvent): Unit = {
        notification.expire()
        ProjectPropertiesComponent.getInstance(project).setValue(ShowUnlinkedProjectNotification, false, true)
      }
    })
    notification.notify(project)
  }
}

object SbtUnlinkedProjectStartupActivity {

  private val ShowUnlinkedProjectNotification = "show.unlinked.sbt.project.notification"

  private def importProject(project: Project, buildFile: Path): Unit = {
    val provider = Application.get().getExtensionPoint(classOf[ModuleImportProvider[?]]).findExtensionOrFail(classOf[SbtProjectImportProvider])
    val result = new CompletableFuture[Pair[ModuleImportContext, ModuleImportProvider[ModuleImportContext]]]()
    Application.get().getInstance(classOf[ModuleCreationHelper])
      .showImportChooser(project, buildFile, Collections.singletonList[ModuleImportProvider[?]](provider), result)

    result.whenComplete { (pair, error) =>
      if (error == null && pair != null) {
        val modifiableModel = ModuleManager.getInstance(project).getModifiableModel
        pair.getSecond.process(pair.getFirst, project, modifiableModel, _ => ())
        WriteAction.runAndWait(() => modifiableModel.commit())
      }
    }
  }
}
