package org.jetbrains.plugins.scala.lang.formatting.scalafmt

import consulo.annotation.component.{ComponentScope, ExtensionImpl, TopicImpl}
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.project.event.ProjectManagerListener
import consulo.project.startup.PostStartupActivity
import consulo.ui.UIAccess

// upstream: one class registered both as postStartupActivity and as ProjectManagerListener;
// Consulo: split into a startup activity and a topic listener
@ExtensionImpl
class ScalafmtProjectListener extends PostStartupActivity with DumbAware {

  override def runActivity(project: Project, uiAccess: UIAccess): Unit = {
    ScalaFmtSuggesterService.instance(using project).init()
    ScalafmtDynamicConfigService.instanceIn(project).init()
  }
}

@TopicImpl(ComponentScope.APPLICATION)
class ScalafmtProjectManagerListener extends ProjectManagerListener {

  override def projectClosing(project: Project): Unit =
    ScalafmtDynamicConfigService.instanceIn(project).clearCaches()
}
