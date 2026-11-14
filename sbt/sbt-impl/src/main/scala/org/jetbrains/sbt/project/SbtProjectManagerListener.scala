package org.jetbrains.sbt.project

import consulo.annotation.component.ExtensionImpl

import consulo.externalSystem.importing.ImportSpecBuilder
import consulo.externalSystem.model.task.ProgressExecutionMode
import consulo.externalSystem.service.project.ExternalSystemProjectRefresher
import consulo.project.Project
import org.jetbrains.plugins.scala.extensions.invokeLater
import org.jetbrains.plugins.scala.startup.ProjectActivity
import org.jetbrains.sbt.project.settings.SbtProjectSettings

@ExtensionImpl
private final class SbtProjectManagerListener extends ProjectActivity {
  override def execute(project: Project): Unit = invokeLater {
    SbtProjectSettings.forProject(project).foreach { settings =>
      if (settings.converterVersion < SbtProjectSettings.ConverterVersion) {
        // TODO Only do this if auto-import is enabled? (more predictable, on the other hand, it's not about "build scripts", as the setting claims)
        ExternalSystemProjectRefresher.getInstance().refreshProjects(new ImportSpecBuilder(project, SbtProjectSystem.Id).use(ProgressExecutionMode.IN_BACKGROUND_ASYNC))
        // The converterVersion will be updated by SbtProjectDataService on a successful refresh.
        settings.converterVersion = SbtProjectSettings.ConverterVersion // TODO Remove (don't trigger another refresh in any case)
      }
    }
  }
}
