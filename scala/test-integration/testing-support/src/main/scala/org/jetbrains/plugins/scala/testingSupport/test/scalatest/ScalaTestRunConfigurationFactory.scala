package org.jetbrains.plugins.scala
package testingSupport.test.scalatest

import consulo.execution.configuration.ConfigurationType
import consulo.execution.configuration.RunConfiguration
import consulo.project.Project
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfigurationFactory

class ScalaTestRunConfigurationFactory(override val typ: ConfigurationType)
  extends AbstractTestRunConfigurationFactory(typ) {

  override def id: String = "ScalaTest"

  override def createTemplateConfiguration(project: Project): RunConfiguration =
    new ScalaTestRunConfiguration(project, this, name = "")
}