package org.jetbrains.bsp.project.test.environment

import consulo.annotation.component.ExtensionImpl
import consulo.execution.configuration.RunConfiguration
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration
import org.jetbrains.plugins.scala.testingSupport.test.testdata.{AllInPackageTestData, ClassTestData}

import scala.jdk.CollectionConverters._

// upstream: lives in testing-support (org.jetbrains.plugins.scala.testingSupport.bsp), here bsp-impl depends on
// testing-support so testing-support stays independent of BSP
@ExtensionImpl
final class BspEnvironmentScalaTestRunnerExtension extends BspEnvironmentRunnerExtension {
  override def runConfigurationSupported(config: RunConfiguration): Boolean =
    config.isInstanceOf[ScalaTestRunConfiguration]

  override def environmentType: ExecutionEnvironmentType = ExecutionEnvironmentType.TEST

  override def classes(config: RunConfiguration): Option[List[String]] = {
    config match {
      case scalaTestConfig: ScalaTestRunConfiguration =>
        scalaTestConfig.testConfigurationData match {
          case data: AllInPackageTestData => Some(data.classBuf.asScala.toList)
          case data: ClassTestData => Some(List(data.testClassPath))
          case _ => None
        }
      case _ => None
    }
  }
}
