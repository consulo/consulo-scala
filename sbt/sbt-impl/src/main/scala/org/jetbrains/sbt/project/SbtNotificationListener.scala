package org.jetbrains.sbt
package project

import consulo.annotation.component.ExtensionImpl


import consulo.externalSystem.model.task.{ExternalSystemTaskId, ExternalSystemTaskNotificationListenerAdapter, ExternalSystemTaskType}
import consulo.project.ui.notification.NotificationType
import org.jetbrains.plugins.scala.project.ScalaProjectConfigurationUtil
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups
import org.jetbrains.sbt.api.RichBoolean

// TODO Rely on the immediate UI interaction API when IDEA-123007 will be implemented
@ExtensionImpl
class SbtNotificationListener extends ExternalSystemTaskNotificationListenerAdapter {
  override def onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean): Unit = {
    // TODO this check must be performed in the External System itself (see SCL-7405)
    if (isSbtProject(id)) {
      processOutput(text)
    }
  }

  override def onStart(id: ExternalSystemTaskId): Unit = {
    if (id.getType != ExternalSystemTaskType.RESOLVE_PROJECT) return
    val project = id.findProject()
    if (project == null) return

    ScalaProjectConfigurationUtil.refreshEditorNotifications(project)
  }

  override def onEnd(id: ExternalSystemTaskId): Unit = {
    if (id.getType != ExternalSystemTaskType.RESOLVE_PROJECT) return
    val project = id.findProject()
    if (project == null) return

    ScalaProjectConfigurationUtil.refreshEditorNotifications(project)
    SbtProjectImportStateService.instance(project).reset()
  }

  private def processOutput(text: String): Unit = {
    text match {
      case WarningMessage(message) =>
        val title = SbtBundle.message("sbt.project.import")
        //noinspection ReferencePassedToNls
        ScalaNotificationGroups.sbtProjectImport.createNotification(title, message, NotificationType.WARNING, null).notify(null)
      case _ => // do nothing
    }
  }

  private def isSbtProject(id: ExternalSystemTaskId): Boolean = id.getProjectSystemId == SbtProjectSystem.Id
}

object WarningMessage {
  private val Prefix = "#warning: "

  def apply(text: String): String = Prefix + text

  def unapply(text: String): Option[String] = text.startsWith(Prefix).option(text.substring(Prefix.length))
}
