package org.jetbrains.plugins.scala.editor

import consulo.localize.LocalizeValue
import consulo.webBrowser.BrowserUtil
import com.intellij.notification.CollapseActionsDirection
import consulo.project.ui.notification.{Notification, NotificationAction, NotificationType}
import consulo.ui.ex.action.AnActionEvent
import consulo.project.Project
import org.jetbrains.plugins.scala.settings.{ScalaApplicationSettings, ScalaEditorSmartKeysConfigurable, ShowSettingsUtilImplExt}
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups

object AutoBraceAdvertiser {
  def advertiseAutoBraces(project: Project): Unit = {
    if (shouldAdvertiseAutoBraces) {
      suggestAutoBraces(project)
      disableNotification()
    }
  }

  def disableNotification(): Unit =
    ScalaApplicationSettings.getInstance.SUGGEST_AUTOBRACE_INSERTION = false

  def shouldAdvertiseAutoBraces: Boolean =
    isNotificationEnabled && ScalaApplicationSettings.getInstance.HANDLE_BLOCK_BRACES_INSERTION_AUTOMATICALLY

  private def isNotificationEnabled: Boolean =
    ScalaApplicationSettings.getInstance.SUGGEST_AUTOBRACE_INSERTION

  private val AutoBraceHttpsBlogPage =
    "https://blog.jetbrains.com/scala/2020/07/22/intellij-scala-plugin-2020-2-indentation-based-brace-handling/"

  private def suggestAutoBraces(project: Project): Unit = {
    val notification = {
      val group = ScalaNotificationGroups.scalaFeaturesAdvertiser
      group.createNotification(ScalaEditorBundle.message("the.curly.braces.can.be.added.or.removed.automatically"), NotificationType.INFORMATION)
    }

    notification.setCollapseDirection(Notification.CollapseActionsDirection.KEEP_LEFTMOST)

    notification
      .addAction(new OpenSettingsAction())
      .addAction(new MoreInfoAction())

    notification.notify(project)
  }

  private class OpenSettingsAction extends NotificationAction(LocalizeValue.of(ScalaEditorBundle.message("auto.braces.advertiser.settings"))) {
    override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
      notification.hideBalloon()
      ShowSettingsUtilImplExt.showSettingsDialog(
        e.getData(Project.KEY),
        classOf[ScalaEditorSmartKeysConfigurable],
        ""
      )
    }
  }

  private class MoreInfoAction extends NotificationAction(LocalizeValue.of(ScalaEditorBundle.message("auto.braces.advertiser.more.info"))) {
    override def actionPerformed(e: AnActionEvent, notification: Notification): Unit =
      BrowserUtil.browse(AutoBraceHttpsBlogPage)
  }
}
