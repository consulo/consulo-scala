package org.jetbrains.plugins.scala
package testingSupport.test.utest

import consulo.execution.configuration.ConfigurationFactory
import consulo.project.Project
import com.intellij.java.language.psi.PsiClass
import org.jetbrains.plugins.scala.testingSupport.test.CustomTestRunnerBasedStateProvider.TestFrameworkRunnerInfo
import org.jetbrains.plugins.scala.testingSupport.test._

class UTestRunConfiguration(
  project: Project,
  configurationFactory: ConfigurationFactory,
  name: String
) extends AbstractTestRunConfiguration(
  project,
  configurationFactory,
  name
) {

  override val testFramework: UTestTestFramework = UTestTestFramework()

  override val configurationProducer: UTestConfigurationProducer = UTestConfigurationProducer()

  override protected val validityChecker: SuiteValidityChecker = new SuiteValidityCheckerBase {
    override protected def isValidClass(clazz: PsiClass): Boolean = UTestTestFramework.isValidUTestSuiteCandidate(clazz)
    override protected def hasSuitableConstructor(clazz: PsiClass): Boolean = true
  }

  override def runStateProvider: RunStateProvider = new CustomTestRunnerOrSbtShellStateProvider(
    this,
    TestFrameworkRunnerInfo(classOf[org.jetbrains.plugins.scala.testingSupport.uTest.UTestRunner]),
    new UTestSbtTestRunningSupport()
  )
}
