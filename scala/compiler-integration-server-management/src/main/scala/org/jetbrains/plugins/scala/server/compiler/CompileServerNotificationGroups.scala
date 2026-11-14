package org.jetbrains.plugins.scala.server.compiler
import consulo.annotation.component.ExtensionImpl
import consulo.localize.LocalizeValue
import consulo.project.ui.notification.{NotificationDisplayType, NotificationGroup, NotificationGroupContributor}

import java.util.function.Consumer

object CompileServerNotificationGroups {
  // upstream: notifications are created with the plain group id "Scala Compile Server" (not registered)
  val compileServer: NotificationGroup =
    new NotificationGroup("Scala Compile Server", LocalizeValue.of(ServerManagementBundle.message("scala.compile.server.title")), NotificationDisplayType.BALLOON, true)
}

@ExtensionImpl
final class CompileServerNotificationGroupContributor extends NotificationGroupContributor {
  override def contribute(consumer: Consumer[NotificationGroup]): Unit =
    consumer.accept(CompileServerNotificationGroups.compileServer)
}
