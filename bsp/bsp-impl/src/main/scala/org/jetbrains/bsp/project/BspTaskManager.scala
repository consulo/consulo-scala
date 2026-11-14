package org.jetbrains.bsp.project

import consulo.externalSystem.model.task.{ExternalSystemTaskId, ExternalSystemTaskNotificationListener}
import consulo.externalSystem.task.ExternalSystemTaskManager
import org.jetbrains.bsp.settings.BspExecutionSettings

import java.util

class BspTaskManager extends ExternalSystemTaskManager[BspExecutionSettings] {
  override def executeTasks(id: ExternalSystemTaskId, taskNames: util.List[String], projectPath: String, settings: BspExecutionSettings,
                            vmOptions: util.List[String], scriptParameters: util.List[String], debuggerSetup: String,
                            listener: ExternalSystemTaskNotificationListener): Unit = {}

  // TODO bsp should be able to support this
  override def cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean = false
}
