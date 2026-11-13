package org.jetbrains.plugins.scala.util

import consulo.annotation.component.ExtensionImpl
import consulo.project.ui.notification.{NotificationGroup, NotificationGroupContributor}

import java.util.function.Consumer

@ExtensionImpl
final class ScalaNotificationGroupContributor extends NotificationGroupContributor {
  override def contribute(consumer: Consumer[NotificationGroup]): Unit =
    ScalaNotificationGroups.all.foreach(consumer.accept)
}
