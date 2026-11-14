package org.jetbrains.sbt.runner

import consulo.execution.configuration.ConfigurationFactory
import com.intellij.execution.configurations.{ConfigurationType, RunConfiguration}
import consulo.project.Project

/**
 * Configuration factory for [[SbtConfigurationType]]
 * @param typez type of configuration supported by this factory.
 */
class SbtRunConfigurationFactory(val typez: ConfigurationType) extends ConfigurationFactory(typez) {

  //should be equal to the default value of SbtConfigurationType.getDisplayName
  override def getId: String = "sbt Task"

  override def createTemplateConfiguration(project: Project): RunConfiguration = new SbtRunConfiguration(project, this, "")
}