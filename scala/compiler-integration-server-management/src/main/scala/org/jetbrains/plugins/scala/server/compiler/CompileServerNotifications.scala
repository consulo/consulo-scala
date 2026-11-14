package org.jetbrains.plugins.scala.server.compiler
import consulo.localize.LocalizeValue
import consulo.project.ui.notification.{NotificationService, NotificationType}
import consulo.project.Project
import consulo.project.ui.wm.WindowManager
import consulo.ui.annotation.{RequiredUIAccess => RequiresEdt}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.api.NlsString

private object CompileServerNotifications {
  /**
   * This method only shows the balloon, but doesn't log it in the "Event Log" tool window.
   */
  // upstream: positions the balloon over the widget component (or the right-bottom corner of the frame) with PositionTracker
  @RequiresEdt
  private def showBalloonNotificationOnWidget(message: NlsString, project: Project): Unit =
    Option(WindowManager.getInstance.getStatusBar(project)) match {
      case Some(statusBar) =>
        statusBar.notifyByBalloon(CompileServerWidgetFactory.ID, NotificationType.INFORMATION, message.nls)
      case None =>
        showNotification(message.nls, NotificationType.INFORMATION, Some(project))
    }

  @Nls
  private def title: String = ServerManagementBundle.message("scala.compile.server.title")

  def showNotification(@Nls message: String, notificationType: NotificationType, project: Option[Project]): Unit =
    NotificationService.getInstance
      .newOfType(CompileServerNotificationGroups.compileServer, notificationType)
      .title(LocalizeValue.of(title))
      .content(LocalizeValue.of(message))
      .notify(project.orNull)

  @RequiresEdt
  def showStoppedByIdleTimeoutNotification(project: Project): Unit = {
    val message = NlsString(ServerManagementBundle.message("compile.server.stopped.due.to.inactivity"))
    showBalloonNotificationOnWidget(message, project)
  }
}
