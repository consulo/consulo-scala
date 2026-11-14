package org.jetbrains.plugins.scala
package testingSupport.test.specs2

import consulo.execution.configuration.ConfigurationType
import consulo.execution.configuration.RunConfiguration
import consulo.project.Project
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfigurationFactory

class Specs2RunConfigurationFactory(override val typ: ConfigurationType)
  extends AbstractTestRunConfigurationFactory(typ) {

  override def id: String = "Specs2"

  override def createTemplateConfiguration(project: Project): RunConfiguration =
    new Specs2RunConfiguration(project, this, "")
}