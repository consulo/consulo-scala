package org.jetbrains.plugins.scala.components

import consulo.project.Project
import consulo.module.content.layer.event.{ModuleRootEvent, ModuleRootListener}

private final class ScalaHighlightingModeWidgetModuleRootListener(project: Project) extends ModuleRootListener {
  override def rootsChanged(event: ModuleRootEvent): Unit = {
    // upstream: reacts only on workspace model changes, Consulo has no workspace model
    ScalaHighlightingModeWidgetUpdater.scheduleWidgetUpdate(project)
  }
}
