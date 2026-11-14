package org.jetbrains.plugins.scala.munit.testingSupport.test.munit
import consulo.java.execution.configurations.OwnJavaParameters
import consulo.process.cmd.ParametersList
import consulo.execution.runner.{ExecutionEnvironment, ProgramRunner}
import consulo.execution.test.TestSearchScope
import consulo.process.ExecutionException
import consulo.execution.ExecutionResult
import com.intellij.java.execution.impl.JavaTestFrameworkRunnableState
import consulo.execution.executor.Executor
import consulo.logging.Logger
import consulo.module.Module
import com.intellij.rt.execution.junit.JUnitStarter
import consulo.process.ProcessHandler
import consulo.execution.process.ProcessTerminatedListener
import consulo.process.ProcessHandlerBuilder
import consulo.util.io.ClassPathUtil
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.testingSupport.test.exceptions
import org.jetbrains.plugins.scala.munit.testingSupport.test.munit.MUnitCommandLineState.Log
import org.jetbrains.plugins.scala.testingSupport.test.testdata.{AllInPackageTestData, TestConfigurationData}
import org.jetbrains.plugins.scala.testingSupport.test.utils.RawProcessOutputDebugLogger

import scala.jdk.CollectionConverters.SeqHasAsJava

private class MUnitCommandLineState(
  conf: MUnitConfiguration,
  env: ExecutionEnvironment,
  failedTests: Option[Seq[(String, String)]]
) extends JavaTestFrameworkRunnableState[MUnitConfiguration](env) {

  override def getFrameworkName: String = "MUnit"

  override def getFrameworkId: String = "munit"

  override def getConfiguration: MUnitConfiguration = conf

  override def getScope: TestSearchScope = testData.searchTest.toPlatformTestSearchScope

  private def testData: TestConfigurationData = conf.testConfigurationData

  override def configureRTClasspath(javaParameters: OwnJavaParameters): Unit = {
    val classPath = javaParameters.getClassPath
    try {
      classPath.addFirst(ClassPathUtil.getJarPathForClass(classOf[JUnitStarter]))
    } catch {
      case noCassDef: NoClassDefFoundError if noCassDef.getMessage.toLowerCase.contains("junit") =>
        Log.warn(noCassDef)
        throw new ExecutionException(MUnitTestingSupportBundle.message("ensure.junit.plugin.is.enabled"))
    }
  }

  // no fork supported for now
  // upstream: JUnitConfiguration.FORK_NONE, the JUnit plugin does not export its packages
  override def getForkMode: String = "none"

  override def passForkMode(forkMode: String, tempFile: java.io.File, parameters: OwnJavaParameters): Unit = ()

  override def passTempFile(parametersList: ParametersList, tempFilePath: String): Unit = {
    // upstream: CompositeParameterTargetedValue (local "@" and a target path), no execution targets in Consulo
    parametersList.add("@" + tempFilePath)
  }

  override def deleteTempFiles(): Unit =
    super.deleteTempFiles()

  @throws[ExecutionException]
  override protected def createJavaParameters: OwnJavaParameters = {
    val javaParameters = super.createJavaParameters

    // upstream: JUnitConfiguration.JUNIT_START_CLASS
    javaParameters.setMainClass(classOf[JUnitStarter].getName)
    javaParameters.getProgramParametersList.add(JUnitStarter.IDE_VERSION + JUnitStarter.VERSION)

    val preferredRunner = JUnitStarter.JUNIT4_PARAMETER // munit works with junit 4
    javaParameters.getProgramParametersList.add(preferredRunner)

    val suitesToTestsMap = failedTests match {
      case Some(failed: Seq[(String, String)]) =>
        val grouped = failed.groupMap(_._1)(_._2).view.mapValues(_.toSet)
        grouped.filter(_._2.nonEmpty)
      case None                                =>
        testData.getTestMap
    }

    import exceptions._
    if (suitesToTestsMap.isEmpty)
      throw configurationException(MUnitTestingSupportBundle.message("munit.command.line.state.no.tests.found"))

    val isSingleClass = suitesToTestsMap.size == 1 && suitesToTestsMap.head._2.isEmpty
    if (isSingleClass) {
      val clazzName = suitesToTestsMap.head._1
      javaParameters.getProgramParametersList.add(clazzName)
    }
    else {
      /** see [[com.intellij.junit4.JUnit4TestRunnerUtil.buildRequest]] */
      val testNames: Seq[String] =
        suitesToTestsMap.flatMap { case (clazz, tests) =>
          if (tests.isEmpty)
            Seq(clazz)
          else
            tests.map(test => s"$clazz,$test")
        }.toSeq

      val packageName = testData.asOptionOf[AllInPackageTestData].fold("")(_.testPackagePath)

      val category = ""
      val filters: String = ""

      createTempFiles(javaParameters)
      JUnitStarter.printClassesList(testNames.asJava, packageName, category, filters, myTempFile)
    }

    javaParameters
  }

  // see com.intellij.execution.junit.TestObject.createHandler
  override protected def createHandler(executor: Executor): ProcessHandler = {
    appendForkInfo(executor)
    val processHandler = ProcessHandlerBuilder.create(createCommandLine()).killable().build()
    ProcessTerminatedListener.attach(processHandler)
    processHandler
  }

  override def execute(executor: Executor, runner: ProgramRunner[?]): ExecutionResult = {
    val result = super.execute(executor, runner)
    RawProcessOutputDebugLogger.maybeAddListenerTo(result.getProcessHandler)
    result
  }
}

object MUnitCommandLineState {
  private val Log = Logger.getInstance(classOf[MUnitCommandLineState])
}
