package org.jetbrains.plugins.scala.repl.console.configuration
import consulo.execution.configuration.{ConfigurationFactory, ConfigurationType, RunConfiguration}
import consulo.project.Project
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.project.*
import org.jetbrains.plugins.scala.api.project.*
import org.jetbrains.plugins.scala.rt.project.*
import org.jetbrains.plugins.scala.shared.project.*

class ScalaConsoleRunConfigurationFactory(val typez: ConfigurationType) extends ConfigurationFactory(typez) {

  // (!!! DO NOT CHANGE IT TO "Scala REPL")
  // Preserve old factory id due to we changed display name to "Scala REPL"
  // Default implementation of `getId` delegates to `getName` which delegates to `myType.getDisplayName`
  @NonNls
  override def getId: String = "Scala Console"

  override def createTemplateConfiguration(project: Project): RunConfiguration = {
    new ScalaConsoleRunConfiguration(project, this, "")
  }

  override def createConfiguration(name: String, template: RunConfiguration): RunConfiguration = {
    val configuration = super.createConfiguration(name, template).asInstanceOf[ScalaConsoleRunConfiguration]
    template.getProject.anyScalaModule.foreach(configuration.setModule)
    configuration
  }
}
