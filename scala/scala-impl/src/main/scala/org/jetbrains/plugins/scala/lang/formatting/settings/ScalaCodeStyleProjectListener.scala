package org.jetbrains.plugins.scala.lang.formatting.settings

import consulo.project.Project
import consulo.project.event.ProjectManagerListener
import org.jetbrains.plugins.scala.lang.formatting.settings.migration.ProjectCodeStyleSettingsMigrationService

private final class ScalaCodeStyleProjectListener extends ProjectManagerListener {

  override def projectOpened(project: Project): Unit = {
    project.getService(classOf[ProjectCodeStyleSettingsMigrationService]).init()
  }
}
