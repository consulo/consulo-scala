package org.jetbrains.plugins.scala
package testingSupport
package test.scalatest

import consulo.annotation.component.ExtensionImpl

import consulo.execution.configuration.ConfigurationFactory
import consulo.execution.configuration.ConfigurationType
import consulo.execution.configuration.ConfigurationTypeUtil
import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import org.jetbrains.plugins.scala.icons.Icons

@ExtensionImpl
class ScalaTestConfigurationType extends ConfigurationType {

  val confFactory = new ScalaTestRunConfigurationFactory(this)

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array[ConfigurationFactory](confFactory)

  override def getDisplayName: LocalizeValue = LocalizeValue.of(TestingSupportBundle.message("scalatest.config.display.name"))

  override def getConfigurationTypeDescription: LocalizeValue = LocalizeValue.of(TestingSupportBundle.message("scalatest.config.description"))

  override def getId: String = "ScalaTestRunConfiguration" //if you want to change id, change it in Android plugin too

  override def getIcon: Image = Icons.SCALA_TEST
}

object ScalaTestConfigurationType {

  @deprecated("use `apply` instead", "2020.3")
  def instance: ScalaTestConfigurationType = apply()

  def apply(): ScalaTestConfigurationType =
    ConfigurationTypeUtil.findConfigurationType(classOf[ScalaTestConfigurationType])
}
