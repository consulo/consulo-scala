package org.jetbrains.plugins.scala.components

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.TopicImpl
import jakarta.inject.Inject

import consulo.project.Project
import consulo.module.content.layer.event.{ModuleRootEvent, ModuleRootListener}

@TopicImpl(ComponentScope.PROJECT)
private final class ScalaHighlightingModeWidgetModuleRootListener @Inject()(project: Project) extends ModuleRootListener {
  override def rootsChanged(event: ModuleRootEvent): Unit = {
    // upstream: reacts only on workspace model changes, Consulo has no workspace model
    ScalaHighlightingModeWidgetUpdater.scheduleWidgetUpdate(project)
  }
}
