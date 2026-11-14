package org.jetbrains.bsp

import consulo.annotation.component.ExtensionImpl
import consulo.project.ui.notification.{NotificationGroup, NotificationGroupContributor}

import java.util.function.Consumer

@ExtensionImpl
final class BspNotificationGroupContributor extends NotificationGroupContributor {
  override def contribute(consumer: Consumer[NotificationGroup]): Unit =
    consumer.accept(BSP.NotificationGroup)
}
