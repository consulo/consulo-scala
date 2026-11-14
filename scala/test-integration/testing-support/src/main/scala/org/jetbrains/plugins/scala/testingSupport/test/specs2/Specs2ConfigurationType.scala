package org.jetbrains.plugins.scala
package testingSupport
package test.specs2

import consulo.annotation.component.ExtensionImpl

import consulo.execution.configuration.ConfigurationFactory
import consulo.execution.configuration.ConfigurationType
import consulo.execution.configuration.ConfigurationTypeUtil
import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import org.jetbrains.plugins.scala.icons.Icons

@ExtensionImpl
class Specs2ConfigurationType extends ConfigurationType {

  val confFactory = new Specs2RunConfigurationFactory(this)

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array[ConfigurationFactory](confFactory)

  override def getDisplayName: LocalizeValue = LocalizeValue.of(TestingSupportBundle.message("specs2.config.display.name"))

  override def getConfigurationTypeDescription: LocalizeValue = LocalizeValue.of(TestingSupportBundle.message("specs2.config.description"))

  override def getId: String = "Specs2RunConfiguration" //if you want to change id, change it in Android plugin too

  override def getIcon: Image = Icons.SCALA_TEST
}

object Specs2ConfigurationType {

  @deprecated("use `apply` instead", "2020.3")
  def instance: Specs2ConfigurationType = apply()

  def apply(): Specs2ConfigurationType =
    ConfigurationTypeUtil.findConfigurationType(classOf[Specs2ConfigurationType])
}