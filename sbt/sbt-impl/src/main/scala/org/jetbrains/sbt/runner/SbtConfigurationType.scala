package org.jetbrains.sbt.runner

import consulo.annotation.component.ExtensionImpl

import consulo.execution.configuration.ConfigurationFactory
import consulo.execution.configuration.ConfigurationType
import consulo.application.dumb.DumbAware
import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import org.jetbrains.sbt.{Sbt, SbtBundle}

/**
 * Configuration running of sbt task.
 */
@ExtensionImpl
class SbtConfigurationType extends ConfigurationType with DumbAware {
  val confFactory = new SbtRunConfigurationFactory(this)
  
  override def getIcon: Image = Sbt.Icon

  override def getDisplayName: LocalizeValue = LocalizeValue.of(SbtBundle.message("sbt.runner.displayName"))

  override def getConfigurationTypeDescription: LocalizeValue = LocalizeValue.of(SbtBundle.message("sbt.runner.description"))

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array[ConfigurationFactory](confFactory)

  override def getId: String = "SbtRunConfiguration"
}