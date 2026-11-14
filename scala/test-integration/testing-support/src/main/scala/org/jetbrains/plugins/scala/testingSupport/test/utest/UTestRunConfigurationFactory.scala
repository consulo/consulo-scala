package org.jetbrains.plugins.scala
package testingSupport.test.utest

import consulo.execution.configuration.ConfigurationType
import consulo.execution.configuration.RunConfiguration
import consulo.project.Project
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfigurationFactory

class UTestRunConfigurationFactory(override val typ: ConfigurationType)
  extends AbstractTestRunConfigurationFactory(typ) {

  override def id: String = "utest"

  override def createTemplateConfiguration(project: Project): RunConfiguration =
    new UTestRunConfiguration(project, this, "")
}