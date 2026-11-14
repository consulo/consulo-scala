package org.jetbrains.plugins.scala.project.maven

import consulo.dataContext.DataContext
import consulo.document.FileDocumentManager
import consulo.project.Project
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.plugins.scala.compiler.sync.SyncAction

private object MavenSyncAction extends SyncAction {
  // what ReimportAction does
  override def syncProject(project: Project, dataContext: DataContext): Unit = {
    FileDocumentManager.getInstance().saveAllDocuments()
    MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles()
  }
}
