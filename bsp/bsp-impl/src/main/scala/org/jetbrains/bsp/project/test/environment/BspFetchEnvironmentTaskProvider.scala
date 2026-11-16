package org.jetbrains.bsp.project.test.environment

import consulo.annotation.component.ExtensionImpl
import consulo.application.util.concurrent.AppExecutorUtil
import consulo.dataContext.DataContext
import consulo.execution.BeforeRunFragment
import consulo.execution.configuration.{ModuleBasedConfiguration, RunConfiguration}
import consulo.execution.runner.ExecutionEnvironment
import consulo.localize.LocalizeValue
import consulo.logging.Logger
import consulo.module.Module
import consulo.ui.UIAccess
import consulo.ui.image.Image
import org.jetbrains.bsp.impl.protocol.BspCommunication
import org.jetbrains.bsp.{BspBundle, Icons}

import java.util.concurrent.CompletableFuture

// upstream: BeforeRunTaskProvider; configureTask (prompting for the BSP target when the task is configured) is not ported -
// the target is asked for when the task is executed and there is more than one candidate
@ExtensionImpl
class BspFetchEnvironmentTaskProvider extends BeforeRunFragment[BspFetchEnvironmentTask] {
  private val logger = Logger.getInstance(classOf[BspCommunication])

  override def getId: String = BspFetchEnvironmentTask.runTaskId

  override def getName: LocalizeValue = LocalizeValue.of(BspBundle.message("bsp.task.name"))

  override def getIcon(configuration: RunConfiguration): Image = Icons.BSP

  override def getStateClass: Class[BspFetchEnvironmentTask] = classOf[BspFetchEnvironmentTask]

  override def createTask(runConfiguration: RunConfiguration): BspFetchEnvironmentTask = {
    runConfiguration match {
      case BspSupportedConfiguration(_, _) => new BspFetchEnvironmentTask
      case _ => null
    }
  }

  override def isSingleton: Boolean = true

  override def executeAsync(
    uiAccess: UIAccess,
    context: DataContext,
    configuration: RunConfiguration,
    env: ExecutionEnvironment,
    task: BspFetchEnvironmentTask
  ): CompletableFuture[?] =
    CompletableFuture.runAsync(() => executeTask(configuration, task), AppExecutorUtil.getAppExecutorService)

  private def executeTask(configuration: RunConfiguration, task: BspFetchEnvironmentTask): Unit =
    try {
      configuration match {
        case BspSupportedConfiguration(config, module) =>
          BspJvmEnvironment.resolveForRun(config, module, task) match {
            case Right(environment) =>
              config.putUserData(BspFetchEnvironmentTask.jvmEnvironmentKey, environment)
            case Left(error) =>
              logger.error(BspBundle.message("bsp.task.error", error.msg))
          }
        case _ => ()
      }
    } catch {
      case error: Throwable => logger.error(error)
    }

  private object BspSupportedConfiguration {
    def unapply(configuration: RunConfiguration): Option[(ModuleBasedConfiguration[?], Module)] = {
      org.jetbrains.bsp.project.test.environment.ModuleBasedConfiguration.unapply(configuration)
        .filter(_ => BspEnvironmentRunnerExtension.isSupported(configuration))
    }
  }
}
