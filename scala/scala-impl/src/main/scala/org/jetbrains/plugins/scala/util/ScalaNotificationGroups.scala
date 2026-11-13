package org.jetbrains.plugins.scala.util

import consulo.localize.LocalizeValue
import consulo.project.ui.notification.{NotificationDisplayType, NotificationGroup}
import org.jetbrains.plugins.scala.ScalaBundle

/**
 * Notification groups of the plugin, registered by [[ScalaNotificationGroupContributor]]
 * (upstream: `notificationGroup` EPs in `scala-plugin-common.xml`).
 */
object ScalaNotificationGroups {
  private def group(id: String, key: String, displayType: NotificationDisplayType): NotificationGroup =
    new NotificationGroup(id, LocalizeValue.of(ScalaBundle.message(key)), displayType, true)

  /**
   * General notification group for any scala-related notification.
   *
   * NOTE: please use custom notification group for every semantically-grouped features instead of scalaGeneral
   */
  val scalaGeneral: NotificationGroup = group("scala.general", "scala.notification.group.id.general", NotificationDisplayType.BALLOON)

  //Balloon (by default)
  val javaToScalaConverter: NotificationGroup = group("java.to.scala.converter", "scala.notification.group.id.java.to.scala.converter", NotificationDisplayType.BALLOON)
  val sbtProjectImport: NotificationGroup = group("sbt.project.import", "scala.notification.group.id.sbt.project.import", NotificationDisplayType.BALLOON)
  val sbtShell: NotificationGroup = group("sbt.shell", "scala.notification.group.id.sbt.shell", NotificationDisplayType.BALLOON)
  val scalaPluginVerifier: NotificationGroup = group("scala.plugin.verifier", "scala.notification.group.id.plugin.verifier", NotificationDisplayType.BALLOON)
  val scalaPluginUpdater: NotificationGroup = group("scala.plugin.updater", "scala.notification.group.id.plugin.updater", NotificationDisplayType.BALLOON)
  val scalafmt: NotificationGroup = group("scalafmt", "scala.notification.group.id.scalafmt", NotificationDisplayType.BALLOON)
  val scalafmtFormatErrors: NotificationGroup = group("scalafmt.format.errors", "scala.notification.group.id.scalafmt.format.errors", NotificationDisplayType.BALLOON)

  //Sticky Balloon (by default)
  val scalaFeaturesAdvertiser: NotificationGroup = group("scala.features.advertiser", "scala.notification.group.id.features.advertiser", NotificationDisplayType.STICKY_BALLOON)
  val scalafmtFatalErrors: NotificationGroup = group("scalafmt.fatal.errors", "scala.notification.group.id.scalafmt.fatal.errors", NotificationDisplayType.STICKY_BALLOON)
  // upstream: registered dynamically via Notifications.TOPIC in LibraryExtensionsManager
  val scalaLibraryExtension: NotificationGroup = new NotificationGroup("Scala Library Extension", NotificationDisplayType.STICKY_BALLOON, true)

  val all: Seq[NotificationGroup] = Seq(
    scalaGeneral, javaToScalaConverter, sbtProjectImport, sbtShell, scalaPluginVerifier, scalaPluginUpdater,
    scalafmt, scalafmtFormatErrors, scalaFeaturesAdvertiser, scalafmtFatalErrors, scalaLibraryExtension
  )
}
