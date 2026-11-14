package org.jetbrains.plugins.scala
package testingSupport.test

import consulo.execution.configuration.ConfigurationFactory
import consulo.execution.configuration.ConfigurationType
abstract class AbstractTestRunConfigurationFactory(val typ: ConfigurationType)
  extends ConfigurationFactory(typ)  {

  def id: String
  override final def getId: String = id
}