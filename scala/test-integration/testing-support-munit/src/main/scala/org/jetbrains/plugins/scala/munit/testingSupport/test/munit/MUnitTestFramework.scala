package org.jetbrains.plugins.scala.munit.testingSupport.test.munit
import com.intellij.java.language.testIntegration.TestFramework
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestFramework.TestFrameworkSetupInfo
import consulo.annotation.component.ExtensionImpl
import org.jetbrains.plugins.scala.testingSupport.test.{AbstractTestFramework, TestFrameworkSetupSupportBase}
import org.jetbrains.plugins.scala.testingSupport.test.munit.{MUnitTestFrameworkMarker, MUnitUtils}

@ExtensionImpl
final class MUnitTestFramework extends AbstractTestFramework
  with TestFrameworkSetupSupportBase
  with MUnitTestFrameworkMarker {

  override def baseSuitePaths: Seq[String] = Seq("munit.Suite")

  override def getMarkerClassFQName: String = "munit.Suite"

  override def getName: String = "MUnit"

  override def getDefaultSuperClass: String = MUnitUtils.FunSuiteFqn

  override def frameworkSetupInfo(scalaVersion: Option[String]): TestFrameworkSetupInfo =
    TestFrameworkSetupInfo(Seq(""""org.scalameta" %% "munit" % "latest.integration" % Test"""), Seq())
}

object MUnitTestFramework {

  def apply(): MUnitTestFramework =
    TestFramework.EXTENSION_NAME.findExtension(classOf[MUnitTestFramework])
}
