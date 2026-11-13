package org.jetbrains.plugins.scala.lang.formatting.settings

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.TopicImpl

import consulo.project.Project
import consulo.project.event.ProjectManagerListener
import org.jetbrains.plugins.scala.lang.formatting.settings.migration.ProjectCodeStyleSettingsMigrationService

@TopicImpl(ComponentScope.APPLICATION)
private final class ScalaCodeStyleProjectListener extends ProjectManagerListener {

  override def projectOpened(project: Project): Unit = {
    project.getInstance(classOf[ProjectCodeStyleSettingsMigrationService]).init()
  }
}
