package org.jetbrains.bsp.junit.project.test.environment

import com.intellij.execution.junit.JUnitConfiguration
import consulo.annotation.component.ExtensionImpl
import consulo.execution.configuration.RunConfiguration
import org.jetbrains.bsp.project.test.environment.{BspEnvironmentRunnerExtension, ExecutionEnvironmentType}

// upstream: @ApiStatus.Internal
@ExtensionImpl
class BspEnvironmentJUnitRunnerExtension extends BspEnvironmentRunnerExtension {
  override def runConfigurationSupported(config: RunConfiguration): Boolean =
    config.isInstanceOf[JUnitConfiguration]

  override def classes(config: RunConfiguration): Option[List[String]] = {
    config match {
      case jUnitConfig: JUnitConfiguration =>
        // upstream: getTestType / getPersistentData - Consulo keeps them in the run configuration options
        jUnitConfig.getOptions.getTestObject match {
          case JUnitConfiguration.TEST_METHOD | JUnitConfiguration.TEST_CLASS =>
            Some(List(jUnitConfig.getOptions.getMainClassName))
          case _ => None
        }
      case _ => None
    }
  }

  override def environmentType: ExecutionEnvironmentType = ExecutionEnvironmentType.TEST
}
