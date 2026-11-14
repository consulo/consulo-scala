package org.jetbrains.plugins.scala.worksheet.utils

import consulo.annotation.component.ExtensionImpl
import consulo.project.ui.notification.{NotificationGroup, NotificationGroupContributor}

import java.util.function.Consumer

@ExtensionImpl
final class WorksheetNotificationGroupContributor extends NotificationGroupContributor {
  override def contribute(consumer: Consumer[NotificationGroup]): Unit =
    consumer.accept(notifications.WorksheetNotificationsGroup)
}
