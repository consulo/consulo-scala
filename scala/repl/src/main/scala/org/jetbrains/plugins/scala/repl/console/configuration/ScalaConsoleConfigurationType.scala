package org.jetbrains.plugins.scala.repl.console.configuration

import consulo.annotation.component.ExtensionImpl
import consulo.execution.configuration.{ConfigurationFactory, ConfigurationType}
import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import org.jetbrains.plugins.scala.repl.console.ScalaReplBundle
import org.jetbrains.plugins.scala.icons.Icons

@ExtensionImpl
class ScalaConsoleConfigurationType extends ConfigurationType {
  private val confFactory = new ScalaConsoleRunConfigurationFactory(this)

  override def getIcon: Image = Icons.SCALA_CONSOLE

  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaReplBundle.message("scala.console.config.display.name"))

  override def getConfigurationTypeDescription: LocalizeValue = LocalizeValue.of(ScalaReplBundle.message("scala.console.config.scala.repl.run.configurations"))

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array[ConfigurationFactory](confFactory)

  override def getId: String = "ScalaScriptConsoleRunConfiguration"
}
