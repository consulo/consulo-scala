package org.jetbrains.plugins.scala.worksheet.utils

import consulo.localize.LocalizeValue
import consulo.project.ui.notification.{NotificationDisplayType, NotificationGroup}
import org.jetbrains.plugins.scala.worksheet.WorksheetBundle

object notifications {

  /**
   * Also see [[org.jetbrains.plugins.scala.util.ScalaNotificationGroups]]<br>
   * upstream: `notificationGroup` EP, registered by [[WorksheetNotificationGroupContributor]]
   */
  val WorksheetNotificationsGroup: NotificationGroup =
    new NotificationGroup(
      "scala.worksheet",
      LocalizeValue.of(WorksheetBundle.message("scala.worksheet.notification.group.display.name")),
      NotificationDisplayType.BALLOON,
      true
    )
}
