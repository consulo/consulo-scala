package org.jetbrains.plugins.scala.lang.formatting

import consulo.localize.LocalizeValue
import consulo.project.ui.notification.{Notification, NotificationAction}
import consulo.ui.ex.action.AnActionEvent
import consulo.navigation.OpenFileDescriptor
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.annotations.Nls

class OpenFileNotificationActon(project: Project, vFile: VirtualFile, offset: Int, @Nls title: String)
  extends NotificationAction(LocalizeValue.of(title)) {

  override def actionPerformed(e: AnActionEvent, notification: Notification): Unit = {
    new OpenFileDescriptor(project, vFile, offset).navigate(true)
    notification.expire()
  }
}