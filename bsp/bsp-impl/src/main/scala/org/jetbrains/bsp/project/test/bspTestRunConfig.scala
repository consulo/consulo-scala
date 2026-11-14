package org.jetbrains.bsp.project.test

import consulo.annotation.component.ExtensionImpl

import consulo.execution.executor.Executor
import consulo.execution.configuration.{ConfigurationFactory, ConfigurationType, RunConfiguration, RunConfigurationBase, RunProfileState}
import consulo.localize.LocalizeValue
import consulo.ui.image.Image
import consulo.execution.runner.ExecutionEnvironment
import consulo.execution.configuration.fragment.RunConfigurationFragment
import consulo.project.Project
import consulo.util.xml.serializer.XmlSerializer
import org.jdom.Element
import org.jetbrains.bsp.project.test.BspTestConfigurationForm._
import org.jetbrains.bsp.{BspBundle, Icons}

import java.net.URI
import java.util.Collections
import java.{util => j}
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@ExtensionImpl
class BspTestRunType extends ConfigurationType {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(BspBundle.message("bsp.test"))

  override def getConfigurationTypeDescription: LocalizeValue = getDisplayName

  override def getIcon: Image = Icons.BSP

  override def getId: String = "BSP_TEST_RUN_CONFIGURATION"

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array(new BspTestRunFactory(this))
}

class BspTestRunFactory(t: ConfigurationType) extends ConfigurationFactory(t) {

  override def getId: String = "BSP test"

  override def createTemplateConfiguration(project: Project): RunConfiguration = new BspTestRunConfiguration(project, this, "BSP_TEST_RUN")

  // upstream: getName = "BspTestRunFactory"
  override def getDisplayName: LocalizeValue = LocalizeValue.of("BspTestRunFactory")
}

class TestClass(@BeanProperty var target: String, @BeanProperty var classes: j.List[String]) {
  def this() = this("", Collections.emptyList())
}

class BspTestRunConfiguration(val project: Project, val configurationFactory: ConfigurationFactory, val name: String)
  extends RunConfigurationBase(project, configurationFactory, name) {


  @BeanProperty var testMode: TestMode = TestMode.ALL_IN_PROJECT
  @BeanProperty var lastTestClassesResponse: j.List[TestClass] = Collections.emptyList()
  @BeanProperty var matchedTestClasses: j.Map[String, j.List[String]] = Collections.emptyMap()
  @BeanProperty var testClassesRegex = ".*"

  override def readExternal(element: Element): Unit = {
    super.readExternal(element)
    XmlSerializer.deserializeInto(this, element)
  }

  override def writeExternal(element: Element): Unit = {
    super.writeExternal(element)
    XmlSerializer.serializeInto(this, element)
  }

  override def checkConfiguration(): Unit = ()

  // upstream: getConfigurationEditor = new BspTestConfigurationForm(getProject)
  override def createFragments(): j.List[RunConfigurationFragment[?]] =
    j.List.of(new BspTestRunConfigurationFragment)

  override def getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = {
    val scalaMap = matchedTestClasses.asScala.map { case (uri, list) => (URI.create(uri), list.asScala.toList) }.toMap
    new BspTestRunner(getProject, this, executor, {
      testMode match {
        case TestMode.ALL_IN_PROJECT => None
        case TestMode.CLASS => Some(scalaMap)
      }
    })
  }
}