package org.jetbrains.bsp.project.test.environment

import consulo.annotation.component.ExtensionImpl
import com.intellij.java.execution.impl.application.ApplicationConfiguration
import consulo.execution.configuration.RunConfiguration

@ExtensionImpl
class BspEnvironmentApplicationRunnerExtension extends BspEnvironmentRunnerExtension {
  override def classes(config: RunConfiguration): Option[List[String]] = {
    config match {
      case applicationConfig: ApplicationConfiguration => Some(List(applicationConfig.getMainClassName))
      case _ => None
    }
  }

  override def runConfigurationSupported(config: RunConfiguration): Boolean =
    config.isInstanceOf[ApplicationConfiguration]

  override def environmentType: ExecutionEnvironmentType = ExecutionEnvironmentType.RUN
}
