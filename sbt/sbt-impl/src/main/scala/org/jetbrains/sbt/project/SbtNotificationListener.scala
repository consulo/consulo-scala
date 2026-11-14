package org.jetbrains.sbt
package project

import consulo.annotation.component.ExtensionImpl
import consulo.application.ApplicationPropertiesComponent
import consulo.externalSystem.model.task.{ExternalSystemTaskId, ExternalSystemTaskNotificationListenerAdapter, ExternalSystemTaskType}
import consulo.ide.setting.ShowSettingsUtil
import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.project.ui.notification.NotificationType
import consulo.ui.ex.action.{AnAction, AnActionEvent}
import org.jetbrains.plugins.scala.project.ScalaProjectConfigurationUtil
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups
import org.jetbrains.sbt.api.RichBoolean
import org.jetbrains.sbt.settings.SbtExternalSystemConfigurable

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

    if (isSbtProject(id)) SbtProjectImportStateService.instance(project).resolveStarted(id)
    ScalaProjectConfigurationUtil.refreshEditorNotifications(project)
  }

  override def onEnd(id: ExternalSystemTaskId): Unit = {
    if (id.getType != ExternalSystemTaskType.RESOLVE_PROJECT) return
    val project = id.findProject()
    if (project == null) return

    val importState = SbtProjectImportStateService.instance(project)
    importState.resolveFinished(id)
    importState.reset()
    ScalaProjectConfigurationUtil.refreshEditorNotifications(project)
  }

  override def onSuccess(id: ExternalSystemTaskId): Unit = {
    if (id.getType != ExternalSystemTaskType.RESOLVE_PROJECT || !isSbtProject(id)) return
    val project = id.findProject()
    if (project == null || project.isDisposed) return

    if (GeneratedManagedSourcesService.instance(project).generatedForAnyPath)
      notifyGeneratedManagedSources(project)
  }

  // upstream: GeneratedManagedSourcesImportListener + SbtTooltip (a GotItTooltip on the sbt tool window stripe button,
  // shown once) - Consulo has no GotItTooltip, a balloon is shown once instead
  private def notifyGeneratedManagedSources(project: Project): Unit = {
    val properties = ApplicationPropertiesComponent.getInstance()
    if (properties.isTrueValue(GeneratedManagedSourcesNotificationShownKey)) return
    properties.setValue(GeneratedManagedSourcesNotificationShownKey, true)

    val notification = ScalaNotificationGroups.sbtProjectImport.createNotification(
      SbtBundle.message("sbt.generated.managed.sources.tooltip.text"),
      NotificationType.INFORMATION
    )
    notification.addAction(new AnAction(LocalizeValue.of(SbtBundle.message("sbt.generated.managed.sources.tooltip.secondary.button.label"))) {
      override def actionPerformed(e: AnActionEvent): Unit = {
        notification.expire()
        ShowSettingsUtil.getInstance().showAndSelect(project, classOf[SbtExternalSystemConfigurable])
      }
    })
    notification.notify(project)
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

  private val GeneratedManagedSourcesNotificationShownKey = "sbt.generated.managed.sources.tooltip"
}

object WarningMessage {
  private val Prefix = "#warning: "

  def apply(text: String): String = Prefix + text

  def unapply(text: String): Option[String] = text.startsWith(Prefix).option(text.substring(Prefix.length))
}
