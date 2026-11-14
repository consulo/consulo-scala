package org.jetbrains.sbt
package project

import consulo.externalSystem.model.task.{ExternalSystemTaskId, ExternalSystemTaskNotificationListener}
import consulo.externalSystem.task.ExternalSystemTaskManager
import org.jetbrains.sbt.project.settings.SbtExecutionSettings

import java.util

class SbtTaskManager extends ExternalSystemTaskManager[SbtExecutionSettings] {
  override def executeTasks(id: ExternalSystemTaskId, taskNames: util.List[String], projectPath: String, settings: SbtExecutionSettings,
                   vmOptions: util.List[String], scriptParameters: util.List[String], debuggerSetup: String, listener: ExternalSystemTaskNotificationListener): Unit = {}

  override def cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean = false
}
