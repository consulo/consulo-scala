package org.jetbrains.plugins.scala
package testingSupport
package test.utest

import consulo.annotation.component.ExtensionImpl

import consulo.execution.configuration.ConfigurationFactory
import consulo.execution.configuration.ConfigurationType
import consulo.execution.configuration.ConfigurationTypeUtil
import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import org.jetbrains.plugins.scala.icons.Icons

@ExtensionImpl
class UTestConfigurationType extends ConfigurationType {
  val confFactory = new UTestRunConfigurationFactory(this)

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array[ConfigurationFactory](confFactory)

  override def getDisplayName: LocalizeValue = LocalizeValue.of(TestingSupportBundle.message("utest.config.display.name"))

  override def getConfigurationTypeDescription: LocalizeValue = LocalizeValue.of(TestingSupportBundle.message("utest.config.description"))

  override def getId: String = "uTestRunConfiguration" //if you want to change id, change it in Android plugin too

  override def getIcon: Image = Icons.SCALA_TEST
}

object UTestConfigurationType {

  @deprecated("use `apply` instead", "2020.3")
  def instance: UTestConfigurationType = apply()

  def apply(): UTestConfigurationType =
    ConfigurationTypeUtil.findConfigurationType(classOf[UTestConfigurationType])
}
