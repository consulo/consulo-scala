package org.jetbrains.sbt.project.settings

import consulo.annotation.component.{ComponentScope, TopicAPI}
import consulo.externalSystem.setting.ExternalSystemSettingsListener

import java.util

/**
 * A dummy to satisfy interface constraints of ExternalSystem
 */
@TopicAPI(ComponentScope.PROJECT)
trait SbtProjectSettingsListener extends ExternalSystemSettingsListener[SbtProjectSettings] {
  // the message bus dispatches only methods declared on the topic interface, so the inherited ones are redeclared
  override def onProjectRenamed(oldName: String, newName: String): Unit = {}

  override def onProjectsLoaded(settings: util.Collection[SbtProjectSettings]): Unit = {}

  override def onProjectsLinked(settings: util.Collection[SbtProjectSettings]): Unit = {}

  override def onProjectsUnlinked(linkedProjectPaths: util.Set[String]): Unit = {}

  override def onBulkChangeStart(): Unit = {}

  override def onBulkChangeEnd(): Unit = {}
}
