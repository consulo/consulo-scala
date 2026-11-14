package org.jetbrains.plugins.scala.munit.testingSupport.test.munit

import consulo.annotation.component.ExtensionImpl
import consulo.execution.configuration.{ConfigurationFactory, ConfigurationType, ConfigurationTypeUtil}
import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import org.jetbrains.plugins.scala.icons.Icons

// upstream: @ApiStatus.Experimental
@ExtensionImpl
final class MUnitConfigurationType extends ConfigurationType {

  val confFactory = new MUnitConfigurationFactory(this)

  override def getId: String = "MUnitRunConfiguration"

  override def getDisplayName: LocalizeValue = LocalizeValue.of(MUnitTestingSupportBundle.message("munit.config.display.name"))

  override def getConfigurationTypeDescription: LocalizeValue = LocalizeValue.of(MUnitTestingSupportBundle.message("munit.config.description"))

  override def getIcon: Image = Icons.SCALA_TEST

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array(confFactory)
}

object MUnitConfigurationType {

  def apply(): MUnitConfigurationType =
    ConfigurationTypeUtil.findConfigurationType(classOf[MUnitConfigurationType])
}
