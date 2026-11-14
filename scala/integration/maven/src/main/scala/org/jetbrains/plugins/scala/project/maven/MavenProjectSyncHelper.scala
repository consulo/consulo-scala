package org.jetbrains.plugins.scala.project.maven

import consulo.annotation.component.ExtensionImpl
import consulo.project.Project
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.plugins.scala.compiler.sync.{BuildToolProjectSyncHelper, ProjectSyncHandler}

//noinspection ApiStatus
@ExtensionImpl
final class MavenProjectSyncHelper extends BuildToolProjectSyncHelper {
  override def projectSyncHandler(project: Project): Option[ProjectSyncHandler] = {
    val manager = MavenProjectsManager.getInstance(project)
    if (manager eq null) return None
    val handles = manager.isMavenizedProject
    if (handles) {
      val syncHandler = ProjectSyncHandler.Custom(MavenSyncAction)
      Some(syncHandler)
    } else None
  }
}
