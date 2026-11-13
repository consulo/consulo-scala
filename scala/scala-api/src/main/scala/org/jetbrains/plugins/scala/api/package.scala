package org.jetbrains.plugins.scala

import consulo.application.Application.{get => application}

package object api {

  val ScalaLowerCase = "scala"
  val NotImplementedError = "???"

  def isInternalMode: Boolean = application.isInternal

  def isUnitTestMode: Boolean = application.isUnitTestMode
}
