package org.jetbrains.sbt.project.versionNotifications

import consulo.navigation.{OpenFileDescriptor, OpenFileDescriptorFactory}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFileManager
import org.jetbrains.plugins.scala.extensions.{OptionExt, PathExt}
import org.jetbrains.sbt.Sbt

import java.nio.file.Path

/**
 * Shared bits used by both legacy-sbt-version workflows:
 *  - [[LegacySbtVersionProjectNotification]] (IDE notifications);
 *  - [[LegacySbtVersionBuildToolWindowWarning]] (Build Tool Window warnings during sbt import).
 *
 * @see SCL-23606
 */
private object LegacySbtVersionUtils {

  val MigrationGuideUrl: String = "https://www.scala-sbt.org/1.x/docs/Migrating-from-sbt-013x.html"

  def createBuildPropertiesOpenFileDescriptor(project: Project, projectRoot: Path): Option[OpenFileDescriptor] = {
    val buildPropertiesFile = projectRoot / Sbt.ProjectDirectory / Sbt.PropertiesFile
    Option(buildPropertiesFile)
      .filter(_.exists).safeMap(VirtualFileManager.getInstance.findFileByNioPath)
      .map(OpenFileDescriptorFactory.getInstance(project).builder(_).offset(0).build())
  }
}
