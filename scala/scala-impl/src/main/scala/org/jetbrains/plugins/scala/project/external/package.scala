package org.jetbrains.plugins.scala.project

import consulo.externalSystem.model.ProjectSystemId
import consulo.externalSystem.service.notification.NotificationData
import consulo.util.dataholder.Key
import org.jetbrains.annotations.TestOnly

package object external {

  @TestOnly
  case class ShownNotification(id: ProjectSystemId, data: NotificationData)

  @TestOnly
  val ShownNotificationsKey: Key[Seq[ShownNotification]] = Key.create("shown.notifications")
}
