package org.jetbrains.plugins.scala.munit.testingSupport.test.munit
import consulo.execution.configuration.RunConfiguration
import consulo.project.Project
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfigurationFactory

final class MUnitConfigurationFactory(typ: MUnitConfigurationType)
  extends AbstractTestRunConfigurationFactory(typ) {

  override def id: String = "MUnit"

  override def createTemplateConfiguration(project: Project): RunConfiguration = {
    val configuration = new MUnitConfiguration(project, this, name = "")
    configuration
  }
}
