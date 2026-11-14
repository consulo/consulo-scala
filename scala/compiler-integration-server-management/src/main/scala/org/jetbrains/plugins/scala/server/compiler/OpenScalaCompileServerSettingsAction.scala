package org.jetbrains.plugins.scala.server.compiler
import consulo.localize.LocalizeValue
import consulo.project.ui.notification.{Notification, NotificationAction}
import consulo.ui.ex.action.AnActionEvent
import consulo.project.Project

private final class OpenScalaCompileServerSettingsAction(project: Project, filter: String)
  extends NotificationAction(LocalizeValue.of(ServerManagementBundle.message("wrong.jdk.action.open.compile.server.settings"))) {
  override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
    notification.expire()
    CompileServerSettingsUtil.showCompileServerSettingsDialog(project, filter)
  }
}
