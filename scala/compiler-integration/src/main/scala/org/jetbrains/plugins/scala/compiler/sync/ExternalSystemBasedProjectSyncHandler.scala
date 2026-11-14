package org.jetbrains.plugins.scala.compiler.sync

import consulo.externalSystem.model.ProjectSystemId
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.module.ModuleManager
import consulo.project.Project

abstract class ExternalSystemBasedProjectSyncHandler(projectSystemId: ProjectSystemId) extends BuildToolProjectSyncHelper {
  override def projectSyncHandler(project: Project): Option[ProjectSyncHandler] = {
    val modules = ModuleManager.getInstance(project).getModules
    val handles = modules.exists(ExternalSystemApiUtil.isExternalSystemAwareModule(projectSystemId, _))
    if (handles) Some(ProjectSyncHandler.ExternalSystem) else None
  }
}
