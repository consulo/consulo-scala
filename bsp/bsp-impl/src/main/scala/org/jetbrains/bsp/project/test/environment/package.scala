package org.jetbrains.bsp.project.test

import consulo.execution.configuration.ModuleBasedConfiguration
import consulo.execution.configuration.RunConfiguration
import consulo.module.Module

package object environment {
  object ModuleBasedConfiguration {
    def unapply(configuration: RunConfiguration): Option[(ModuleBasedConfiguration[?], Module)] = {
      configuration match {
        case config: ModuleBasedConfiguration[?] =>
          val module = config.getConfigurationModule.getModule
          Some(config, module)
        case _ => None
      }
    }
  }
}
