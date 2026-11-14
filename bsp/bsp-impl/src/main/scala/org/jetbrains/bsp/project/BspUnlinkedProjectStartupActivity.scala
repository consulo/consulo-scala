package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl
import consulo.application.dumb.DumbAware
import consulo.externalSystem.model.ExternalSystemDataKeys
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.project.startup.PostStartupActivity
import consulo.ui.UIAccess
import org.jetbrains.bsp.BSP
import org.jetbrains.bsp.project.importing.{BspProjectImportProvider, BspProjectOpenProcessor}
import org.jetbrains.bsp.settings.BspSettings
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.project.autolink.UnlinkedProjectNotification
import org.jetbrains.sbt.settings.SbtSettings

import java.nio.file.{Files, Path}

/**
 * Offers to load a BSP workspace (a `.bsp` connection file, a Bloop configuration, a Mill or Scala CLI project) found in the
 * project base directory, but only if there is no `build.sbt` file - then the sbt notification is shown instead.
 */
// upstream: BspUnlinkedProjectAware (ExternalSystemUnlinkedProjectAware), see UnlinkedProjectNotification
@ExtensionImpl
final class BspUnlinkedProjectStartupActivity extends PostStartupActivity with DumbAware {

  import BspUnlinkedProjectStartupActivity.*

  override def runActivity(project: Project, uiAccess: UIAccess): Unit = {
    val basePath = project.getBasePath
    if (basePath == null ||
      !UnlinkedProjectNotification.isEnabled(project, ShowUnlinkedProjectNotification) ||
      !BspSettings.getInstance(project).getLinkedProjectsSettings.isEmpty ||
      project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) == java.lang.Boolean.TRUE)
      return

    val workspace = Path.of(basePath)
    if (Files.isRegularFile(workspace.resolve(Sbt.BuildFile)) ||
      isLinkedProject(project, basePath) ||
      !BspProjectOpenProcessor.canOpenProject(workspace))
      return

    UnlinkedProjectNotification.show(
      project,
      BSP.NotificationGroup,
      LocalizeValue.localizeTODO("BSP project found"),
      LocalizeValue.localizeTODO("The project directory contains a BSP workspace which is not imported"),
      LocalizeValue.localizeTODO("Load BSP Project"),
      ShowUnlinkedProjectNotification,
      classOf[BspProjectImportProvider],
      workspace
    )
  }
}

object BspUnlinkedProjectStartupActivity {

  private val ShowUnlinkedProjectNotification = "show.unlinked.bsp.project.notification"

  private def isLinkedProject(project: Project, externalProjectPath: String): Boolean =
    BspSettings.getInstance(project).getLinkedProjectSettings(externalProjectPath) != null ||
      SbtSettings.getInstance(project).getLinkedProjectSettings(externalProjectPath) != null
}
