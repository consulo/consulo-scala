package org.jetbrains.bsp

import consulo.externalSystem.model.ProjectSystemId
import consulo.localize.LocalizeValue
import consulo.project.ui.notification.{NotificationDisplayType, NotificationGroup}
import consulo.ui.image.Image
import org.jetbrains.annotations.Nls

object BSP {
  @Nls
  //noinspection ScalaExtractStringToBundle
  val Name = "BSP"
  val Icon: Image = Icons.BSP

  val ProjectSystemId = new ProjectSystemId("BSP", LocalizeValue.of(Name), Icons.BSP)

  // upstream: notificationGroup extension in scalaCommunity.bsp.xml, registered by BspNotificationGroupContributor
  lazy val NotificationGroup: NotificationGroup =
    new NotificationGroup("BSP", LocalizeValue.of(BspBundle.message("scala.notification.group.id.bsp")), NotificationDisplayType.BALLOON, true)
}
