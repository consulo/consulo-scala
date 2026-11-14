package org.jetbrains.plugins.scala.munit.testingSupport.test.munit
import consulo.execution.executor.Executor
import consulo.execution.test.action.AbstractRerunFailedTestsAction
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties
import consulo.execution.test.sm.runner.SMTestLocator
import consulo.execution.ui.console.ConsoleView
import consulo.language.psi.scope.GlobalSearchScope
import org.jetbrains.plugins.scala.testingSupport.test.actions.ScalaRerunFailedTestsAction
import org.jetbrains.plugins.scala.testingSupport.test.munit.MUnitTestLocator

final class MUnitConsoleProperties(configuration: MUnitConfiguration, executor: Executor)
  extends SMTRunnerConsoleProperties(configuration, MUnitTestFramework().getName, executor) {

  override def getTestLocator: SMTestLocator =
    new MUnitTestLocator

  override protected def initScope(): GlobalSearchScope =
    super.initScope()

  override def createRerunFailedTestsAction(consoleView: ConsoleView): AbstractRerunFailedTestsAction =
    new ScalaRerunFailedTestsAction(consoleView, this)
}



