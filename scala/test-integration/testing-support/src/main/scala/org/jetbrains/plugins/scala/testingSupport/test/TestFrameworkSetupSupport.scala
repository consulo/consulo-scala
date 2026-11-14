package org.jetbrains.plugins.scala.testingSupport.test

import consulo.module.Module
import com.intellij.java.language.testIntegration.TestFramework
import consulo.util.concurrent.Promise
import consulo.util.concurrent.Promises
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestFramework.TestFrameworkSetupInfo
import org.jetbrains.sbt.project.modifier.SimpleBuildFileModifier

trait TestFrameworkSetupSupport {
  self: TestFramework =>

  /**
   * Setups test framework in the module in "Create test dialog" if framework is missing from module dependencies
   * @see [[com.intellij.testIntegration.createTest.CreateTestDialog#myFixLibraryButton]]
   */
  def setupFramework(module: Module): Promise[Void]
}

trait TestFrameworkSetupSupportBase extends TestFrameworkSetupSupport {
  self: TestFramework =>

  def frameworkSetupInfo(scalaVersion: Option[String]): TestFrameworkSetupInfo

  /**
   * Setups test framework in the module in "Create test dialog" if framework is missing from module dependencies
   * @see [[com.intellij.testIntegration.createTest.CreateTestDialog#myFixLibraryButton]]
   */
  def setupFramework(module: Module): Promise[Void] = {
    import org.jetbrains.plugins.scala.project._
    import org.jetbrains.plugins.scala.api.project._
    import org.jetbrains.plugins.scala.rt.project._
    import org.jetbrains.plugins.scala.shared.project._
    module.scalaSdk match {
      case Some(sdk) =>
        val setupInfo = frameworkSetupInfo(sdk.libraryVersion)
        val modifier = new SimpleBuildFileModifier(setupInfo.dependencies, Seq(), setupInfo.scalacOptions)
        modifier.modify(module, needPreviewChanges = true)
        Promises.resolvedPromise()
      case None =>
        val message = s"Failed to download test library jars: scala SDK is not specified for module '${module.getName}'"
        Promises.rejectedPromise(message)
    }
  }
}
