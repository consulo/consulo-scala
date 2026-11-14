package org.jetbrains.plugins.scala.testingSupport.test.munit

import com.intellij.java.execution.CommonJavaRunConfigurationParameters

trait DelegateCommonJavaRunConfigurationParameters
  extends DelegateCommonProgramRunConfigurationParameters
    with CommonJavaRunConfigurationParameters {

  // upstream: also a separate "alternative JRE enabled" flag, in Consulo the JRE is enabled when a path is set
  override def isAlternativeJrePathEnabled: Boolean = getAlternativeJrePath != null

  override def getAlternativeJrePath: String = delegateToTestData.jrePath
  override def setAlternativeJrePath(path: String): Unit = delegateToTestData.jrePath = path

  override def setVMParameters(value: String): Unit = delegateToTestData.javaOptions = value
  override def getVMParameters: String = delegateToTestData.javaOptions

  override def getRunClass: String = null
  override def getPackage: String = null
}
