package org.jetbrains.bsp.project.test.environment

import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.execution.event.{RunManagerListener, RunManagerListenerEvent}
import consulo.execution.{BeforeRunTask, RunManager, RunnerAndConfigurationSettings}

// upstream: the RunManager is taken from the project when created, the settings added before it are installed on stateLoaded -
// Consulo passes the RunManager with the event
@TopicImpl(ComponentScope.PROJECT)
class BspFetchTestEnvironmentTaskInstaller extends RunManagerListener {

  override def runConfigurationAdded(event: RunManagerListenerEvent): Unit =
    installFetchEnvironmentTask(event.getSettings, event.getSource)

  private def installFetchEnvironmentTask(settings: RunnerAndConfigurationSettings, runManager: RunManager): Unit = {
    val runConfiguration = settings.getConfiguration
    if (BspEnvironmentRunnerExtension.isSupported(runConfiguration)) {
      val tasks = runManager.getBeforeRunTasks(runConfiguration, classOf[BspFetchEnvironmentTask])
      if (tasks.isEmpty) {
        val task = new BspFetchEnvironmentTask
        task.setEnabled(true)

        val mutableRunTasks = new java.util.ArrayList[BeforeRunTask](runManager.getBeforeRunTasks(runConfiguration))
        mutableRunTasks.add(task)
        runManager.setBeforeRunTasks(runConfiguration, mutableRunTasks, false)
      }
    }
  }
}
