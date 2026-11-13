package org.jetbrains.plugins.scala.lang.formatting.scalafmt

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.TopicImpl

import consulo.annotation.component.ExtensionImpl

import consulo.project.Project
import consulo.project.event.ProjectManagerListener
import consulo.project.startup.StartupActivity
import consulo.ui.UIAccess

//noinspection UsagesOfObsoleteApi
@ExtensionImpl
@TopicImpl(ComponentScope.APPLICATION)
class ScalafmtProjectListener extends StartupActivity.DumbAware with ProjectManagerListener {

  override def runActivity(project: Project, uiAccess: UIAccess): Unit = {
    ScalaFmtSuggesterService.instance(using project).init()
    ScalafmtDynamicConfigService.instanceIn(project).init()
  }

  override def projectClosing(project: Project): Unit =
    ScalafmtDynamicConfigService.instanceIn(project).clearCaches()
}
