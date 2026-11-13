package org.jetbrains.plugins.scala.lang.formatting.scalafmt

import consulo.project.Project
import consulo.project.event.ProjectManagerListener
import consulo.project.startup.StartupActivity

//noinspection UsagesOfObsoleteApi
class ScalafmtProjectListener extends StartupActivity.DumbAware with ProjectManagerListener {

  override def runActivity(project: Project): Unit = {
    ScalaFmtSuggesterService.instance(using project).init()
    ScalafmtDynamicConfigService.instanceIn(project).init()
  }

  override def projectClosing(project: Project): Unit =
    ScalafmtDynamicConfigService.instanceIn(project).clearCaches()
}
