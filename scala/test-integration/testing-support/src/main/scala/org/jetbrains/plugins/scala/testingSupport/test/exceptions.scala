package org.jetbrains.plugins.scala.testingSupport.test

import consulo.process.ExecutionException
import consulo.execution.configuration.RuntimeConfigurationError
import consulo.execution.RuntimeConfigurationException
import org.jetbrains.annotations.Nls

/**
 * These special exceptions are used by the IntelliJ platform for exceptional control flow
 */
// upstream: members are private[test], also used by testing-support-munit
trait exceptions {

  private[scala] final def configurationException(@Nls message: String) = new RuntimeConfigurationException(message)
  private[scala] final def configurationError(@Nls message: String) = new RuntimeConfigurationError(message)
  private[scala] final def executionException(message: String) = new ExecutionException(message)
  private[scala] final def executionException(message: String, cause: Throwable) = new ExecutionException(message, cause)
}

object exceptions extends exceptions