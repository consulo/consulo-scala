package org.jetbrains.sbt.project.autolink

import consulo.annotation.component.ExtensionImpl
import consulo.application.dumb.DumbAware
import consulo.externalSystem.model.ExternalSystemDataKeys
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.project.startup.PostStartupActivity
import consulo.ui.UIAccess
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.project.SbtProjectImportProvider
import org.jetbrains.sbt.settings.SbtSettings

import java.nio.file.{Files, Path}

// upstream: SbtUnlinkedProjectAware (ExternalSystemUnlinkedProjectAware + SbtTextProvider), see UnlinkedProjectNotification
@ExtensionImpl
final class SbtUnlinkedProjectStartupActivity extends PostStartupActivity with DumbAware {

  import SbtUnlinkedProjectStartupActivity.*

  override def runActivity(project: Project, uiAccess: UIAccess): Unit = {
    val basePath = project.getBasePath
    if (basePath == null ||
      !UnlinkedProjectNotification.isEnabled(project, ShowUnlinkedProjectNotification) ||
      !SbtSettings.getInstance(project).getLinkedProjectsSettings.isEmpty ||
      project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) == java.lang.Boolean.TRUE)
      return

    val buildFile = Path.of(basePath).resolve(Sbt.BuildFile)
    if (!Files.isRegularFile(buildFile) || SbtUnlinkedProjectAwareHelper.isLinkedProject(project, basePath))
      return

    UnlinkedProjectNotification.show(
      project,
      ScalaNotificationGroups.sbtProjectImport,
      LocalizeValue.localizeTODO("sbt build script found"),
      LocalizeValue.localizeTODO("The project directory contains a build.sbt file which is not imported"),
      LocalizeValue.localizeTODO("Load sbt Project"),
      ShowUnlinkedProjectNotification,
      classOf[SbtProjectImportProvider],
      buildFile
    )
  }
}

object SbtUnlinkedProjectStartupActivity {

  private val ShowUnlinkedProjectNotification = "show.unlinked.sbt.project.notification"
}
