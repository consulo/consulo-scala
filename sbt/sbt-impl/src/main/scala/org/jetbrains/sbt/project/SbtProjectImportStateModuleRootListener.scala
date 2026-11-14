package org.jetbrains.sbt.project

import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.module.content.layer.event.{ModuleRootEvent, ModuleRootListener}
import consulo.project.Project
import jakarta.inject.Inject

@TopicImpl(ComponentScope.PROJECT)
private final class SbtProjectImportStateModuleRootListener @Inject()(project: Project) extends ModuleRootListener {
  override def rootsChanged(event: ModuleRootEvent): Unit = {
    // upstream: event.isCausedByWorkspaceModelChangesOnly - not in Consulo
    if (!event.isCausedByFileTypesChange && !project.isDisposed) {
      SbtProjectImportStateService.instance(project).reset()
    }
  }
}
