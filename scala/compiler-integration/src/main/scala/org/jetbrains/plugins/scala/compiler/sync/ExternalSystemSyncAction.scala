package org.jetbrains.plugins.scala.compiler.sync

import consulo.dataContext.DataContext
import consulo.project.Project
import consulo.ui.ex.action.{ActionManager, AnActionEvent, AnActionWithSyncUpdate}

private object ExternalSystemSyncAction extends SyncAction {
  // RefreshAllExternalProjectsAction is an internal class, registered by id
  private final val RefreshAllExternalProjectsActionId = "ExternalSystem.RefreshAllProjects"

  override def syncProject(project: Project, dataContext: DataContext): Unit = {
    val refreshAction = ActionManager.getInstance().getAction(RefreshAllExternalProjectsActionId)
    if (refreshAction eq null) return
    val event = AnActionEvent.createFromAnAction(refreshAction, null, "", dataContext)
    refreshAction match {
      case action: AnActionWithSyncUpdate => action.update(event)
      case _ =>
    }
    if (event.getPresentation.isEnabled) {
      refreshAction.actionPerformed(event)
    }
  }
}
