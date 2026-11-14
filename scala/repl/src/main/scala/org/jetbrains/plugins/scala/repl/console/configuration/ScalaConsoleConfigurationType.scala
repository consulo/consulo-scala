package org.jetbrains.plugins.scala.repl.console.configuration

import consulo.annotation.component.ExtensionImpl
import consulo.execution.configuration.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import consulo.application.dumb.DumbAware
import org.jetbrains.plugins.scala.repl.console.ScalaReplBundle
import org.jetbrains.plugins.scala.icons.Icons

import javax.swing.Icon

@ExtensionImpl
class ScalaConsoleConfigurationType extends ConfigurationType with DumbAware {
  private val confFactory = new ScalaConsoleRunConfigurationFactory(this)

  override def getIcon: Icon = Icons.SCALA_CONSOLE

  override def getDisplayName: String = ScalaReplBundle.message("scala.console.config.display.name")

  override def getConfigurationTypeDescription: String = ScalaReplBundle.message("scala.console.config.scala.repl.run.configurations")

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array[ConfigurationFactory](confFactory)

  override def getId: String = "ScalaScriptConsoleRunConfiguration"
}