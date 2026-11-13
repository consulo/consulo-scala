package org.jetbrains.plugins.scala.components.libextensions.ui

import consulo.localize.LocalizeValue
import consulo.project.ui.notification.event.NotificationListener
import consulo.project.ui.notification.{Notification, NotificationService}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups

import javax.swing.event.HyperlinkEvent

class PopupHelper {
  def showEnablePopup(yesCallback: () => Unit, noCallback: () => Unit): Unit = {
    NotificationService.getInstance
      .newInfo(ScalaNotificationGroups.scalaLibraryExtension)
      .title(LocalizeValue.of(ScalaBundle.message("title.extensions.available")))
      .content(LocalizeValue.of(ScalaBundle.message("additional.support.has.been.found.popup")))
      .hyperlinkListener(new NotificationListener {
        override def hyperlinkActivated(notification: Notification, event: HyperlinkEvent): Unit = {
          notification.expire()
          event.getDescription match {
            case "Yes" => yesCallback()
            case "No"  => noCallback()
          }
        }
      })
      .notify(null)
  }
}

object PopupHelper {
  val GROUP_ID = "Scala Library Extension"
}
