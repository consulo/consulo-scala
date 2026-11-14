package org.jetbrains.plugins.scala.munit.testingSupport.test.munit
import consulo.execution.action.{ConfigurationFromContext, RunConfigurationProducer}
import consulo.execution.configuration.ConfigurationFactory
import consulo.util.lang.StringUtil
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScTemplateDefinition, ScTypeDefinition}
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestConfigurationProducer
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestConfigurationProducer.CreateFromContextInfo
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestConfigurationProducer.CreateFromContextInfo.{AllInPackage, ClassWithTestName}
import org.jetbrains.plugins.scala.testingSupport.test.TestConfigurationUtil.isInheritor
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestSingleTestLocationFinderOld
import consulo.annotation.component.ExtensionImpl

@ExtensionImpl
final class MUnitConfigurationProducer extends AbstractTestConfigurationProducer[MUnitConfiguration](MUnitConfigurationType().confFactory) {

  override val suitePaths: Seq[String] = MUnitTestFramework().baseSuitePaths

  override def shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean = {
    //JUnit run configuration producer can also create configuration for MUnit test because MUnit reuses JUnit under the hood
    //In this case we prioritise MUnit configuration (see SCL-18441)
    // upstream: isInstanceOf[JUnitConfigurationType], the JUnit plugin does not export its packages
    other.getConfigurationType.getId == MUnitConfigurationProducer.JUnitConfigurationTypeId
  }

  override protected def configurationName(contextInfo: CreateFromContextInfo): String = contextInfo match {
    case AllInPackage(_, packageName) =>
      s"MUnit in '$packageName'"
    case ClassWithTestName(testClass, testName) =>
      StringUtil.getShortName(testClass.qualifiedName) + testName.fold("")("." + _)
  }

  override def getTestClassWithTestName(location: PsiElementLocation): Option[ClassWithTestName] = {
    val element = location.getPsiElement

    element match {
      case file: ScalaFile =>
        val clazz = file.typeDefinitions.filter(matchesSomeTestSuite) match {
          case Seq(testClass) => Some(testClass) // run multiple test classes in a file is not supported yet, see SCL-15567
          case _ => None
        }
        return clazz.map(ClassWithTestName(_, None))
      case _ =>
    }

    def matchesSomeTestSuite(typ: ScTemplateDefinition): Boolean = suitePaths.exists(isInheritor(typ, _))

    import org.jetbrains.plugins.scala.extensions.IteratorExt
    val parentClasses = element.withParentsInFile.filterByType[ScTypeDefinition]
    val suiteClass = parentClasses.filter(matchesSomeTestSuite).nextOption() match {
      case Some(value) =>
        value
      case _ =>
        return None
    }

    val testName = suiteClass.extendsBlock.templateBody match {
      case Some(templateBody) =>
        val finder = new ScalaTestSingleTestLocationFinderOld(element, suiteClass, templateBody)
        finder.findTestNameForMUnit()
      case _ =>
        None
    }
    val result = ClassWithTestName(suiteClass, testName)
    Some(result)
  }
}

object MUnitConfigurationProducer {

  def apply(): MUnitConfigurationProducer =
    RunConfigurationProducer.getInstance(classOf[MUnitConfigurationProducer])

  // see com.intellij.execution.junit.JUnitConfigurationType
  private val JUnitConfigurationTypeId = "JUnit"
}