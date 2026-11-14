package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.annotation.component.ExtensionImpl

import consulo.project.ui.notification.{NotificationType, Notifications}
import consulo.ui.ex.action.{ActionManager, ActionUpdateThread, AnAction, AnActionEvent}
import com.intellij.openapi.actionSystem.AnActionResult
import consulo.ui.ex.action.event.AnActionListener
import consulo.application.ApplicationManager
import consulo.ide.setting.ShowSettingsUtil
import consulo.project.Project
import consulo.project.startup.StartupActivity
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings
import org.jetbrains.plugins.scala.settings.sections.XRayModeSettingsSectionConfigurable
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups

@ExtensionImpl
class XRayModeTip extends StartupActivity.DumbAware {
  private var messageBusConnection: Option[MessageBusConnection] = None

  override def runActivity(project: Project): Unit =  {
    if (isEnabled && messageBusConnection.isEmpty) {
      messageBusConnection = Some {
        ApplicationManager.getApplication.getMessageBus.connect()
      }
      messageBusConnection.foreach(_.subscribe(AnActionListener.TOPIC, actionListener))
    }
  }

  private def actionListener: AnActionListener = {
    new AnActionListener() {
      def isAction(actionId: String, action: AnAction): Boolean =
        action == ActionManager.getInstance.getAction(actionId).ensuring(_ != null, actionId)

      override def afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult): Unit = {
        if (isEnabled) {
          if (
            isAction("Scala.TypeInfo", action) ||
            isAction("Scala.ShowImplicitConversions", action) || isAction("Scala.ShowImplicitArguments", action) || isAction("Scala.ShowImplicits", action)) {

            suggestXRayMode(isAction("Scala.TypeInfo", action))
            disable()
          }
        }
      }
    }
  }

  private def isEnabled: Boolean =
    ScalaApplicationSettings.getInstance.SUGGEST_XRAY_MODE

  private def disable(): Unit = {
    ScalaApplicationSettings.getInstance.SUGGEST_XRAY_MODE = false
    messageBusConnection.foreach(_.disconnect())
    messageBusConnection = None
  }

  private def suggestXRayMode(typeInfo: Boolean): Unit = {
    val notification = ScalaNotificationGroups.scalaFeaturesAdvertiser.createNotification(
      if (typeInfo) ScalaCodeInsightBundle.message("xray.mode.tip.title.type.hints")
      else ScalaCodeInsightBundle.message("xray.mode.tip.title.implicit.hints"),
      ScalaHintsSettings.xRayModeShortcut.capitalize,
      NotificationType.INFORMATION)

    notification.addAction(new AnAction(ScalaCodeInsightBundle.message("xray.mode.tip.action.got.it")) {
      override def actionPerformed(e: AnActionEvent): Unit = {
        disable()
        notification.hideBalloon()
      }

      override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
    })

    notification.addAction(new AnAction(ScalaCodeInsightBundle.message("xray.mode.tip.action.configure")) {
      override def actionPerformed(e: AnActionEvent): Unit = {
        disable()
        notification.hideBalloon()

        ShowSettingsUtil.getInstance.showSettingsDialog(e.getProject, classOf[XRayModeSettingsSectionConfigurable])
      }

      override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
    })

    Notifications.Bus.notify(notification)
  }
}
