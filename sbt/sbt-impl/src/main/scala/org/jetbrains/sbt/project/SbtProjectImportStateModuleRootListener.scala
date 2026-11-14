package org.jetbrains.sbt.project

import consulo.module.content.layer.event.{ModuleRootEvent, ModuleRootListener}

private final class SbtProjectImportStateModuleRootListener extends ModuleRootListener {
  override def rootsChanged(event: ModuleRootEvent): Unit = {
    val project = event.getProject
    if (event.isCausedByWorkspaceModelChangesOnly && !project.isDisposed) {
      SbtProjectImportStateService.instance(project).reset()
    }
  }
}
