package org.jetbrains.plugins.scala.testingSupport
package test.ui

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import com.intellij.java.language.codeInsight.TestFrameworks
import consulo.execution.test.TestStateStorage
import consulo.execution.lineMarker.{ExecutorAction, RunLineMarkerContributor}
import consulo.execution.test.TestIconMapper
import consulo.execution.icon.ExecutionIconGroup
import consulo.execution.test.TestStateInfo.Magnitude
import consulo.ui.image.Image
import consulo.project.Project
import com.intellij.java.language.psi.{PsiClass, PsiMethod}
import consulo.language.psi.PsiElement
import consulo.language.impl.psi.LeafPsiElement
import consulo.language.psi.util.PsiTreeUtil
import com.intellij.java.language.testIntegration.TestFramework
import com.intellij.java.execution.impl.testframework.TestRunLineMarkerProvider
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.incremental.Highlighting.ElementHighlightingExt
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject, ScTrait, ScTypeDefinition}
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestFramework
import org.jetbrains.plugins.scala.testingSupport.test.munit.{MUnitTestFrameworkMarker, MUnitTestLocationsFinder, MUnitUtils}
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.{ScalaTestTestFramework, ScalaTestTestLocationsFinder}
import org.jetbrains.plugins.scala.testingSupport.test.specs2.{Specs2TestFramework, Specs2TestLocationsFinder}
import org.jetbrains.plugins.scala.testingSupport.test.utest.UTestTestFramework

import scala.jdk.CollectionConverters.IterableHasAsScala

/**
 * ATTENTION: despite the name "ScalaTest*" this line marker provider is applicable for all test frameworks
 *
 * @todo split providers by test frameworks, if some logic should be reused, just move to some base/utility classes
 */
@ExtensionImpl
class ScalaTestRunLineMarkerProvider extends TestRunLineMarkerProvider {
  override def getLanguage: Language = ScalaLanguage.INSTANCE


  /**
   * for scalatest location hint format see:
   *  - [[org.jetbrains.plugins.scala.testingSupport.scalaTest.ScalaTestReporter]]
   *  - [[org.jetbrains.plugins.scala.testingSupport.scalaTest.ScalaTestReporterWithLocation]]
   *  - [[org.jetbrains.plugins.scala.testingSupport.locationProvider.ScalaTestLocationProvider]]
   *
   * for uTest location hint format see
   *  - [[org.jetbrains.plugins.scala.testingSupport.uTest.UTestReporter]]
   */
  override def getInfo(element: PsiElement): RunLineMarkerContributor.Info = {
    val file = element.getContainingFile
    val project = if (file != null) file.getProject else element.getProject // Avoid tree walk-up

    if (!ScalaProjectSettings.in(project).isDisableInspections) {
      if (!element.isVisible(project, file)) return null
    }

    element match {
      case leaf: LeafPsiElement if leaf.getElementType == ScalaTokenTypes.tIDENTIFIER =>
        infoForLeafElement(leaf).orNull
      case _ =>
        null
    }
  }

  // REMINDER from codeInsight.daemon.LineMarkerInfo:
  // LineMarker is supposed to be registered for leaf elements only!
  private def infoForLeafElement(leaf: LeafPsiElement): Option[RunLineMarkerContributor.Info] = {
    val parent = leaf.getParent
    parent match {
      case o: ScObject =>
        infoForScalaTestRefSpec(o) match {
          case info@Some(_) => info
          case _ => infoForClass(o)
        }
      case clazz: PsiClass => infoForClass(clazz)
      case method: PsiMethod => infoForMethod(method)
      case ref: ScReferenceExpression => infoForTestMethodRef(ref)
      case _ => None
    }
  }

  private def infoForClass(clazz: PsiClass): Option[RunLineMarkerContributor.Info] = {
    val framework = TestFrameworks.detectFramework(clazz)
    if (framework == null || !framework.isTestClass(clazz))
      return None

    val url = framework match {
      case _: ScalaTestTestFramework =>
        clazz match {
          case _: ScTrait | _: ScObject => None
          case _ => Some(s"scalatest://TopOfClass:${clazz.qualifiedName}TestName:${clazz.name}")
        }
      //FIXME: why the hell uTest url is reported with scalatest prefix? (see UTestReporter)
      case _: UTestTestFramework => Some(s"scalatest://TopOfClass:${clazz.qualifiedName}TestName:${clazz.qualifiedName}") // TODO: check maybe it should be clazz.name
      case _: Specs2TestFramework => Some(s"") // TODO: spec2 runner does not report location for class currently
      case _ => Some(s"java:suite://${clazz.qualifiedName}")
    }
    url.map(buildLineInfo(_, clazz.getProject, isClass = true))
  }

  // REMINDER: we shouldn't do any long resolve or some other heavy computation,
  // because we can't block UI thread, we already have a lag in a context menu on a single method (right click),
  // not talking about dozens of methods which should be resolved here
  private def infoForMethod(method: PsiMethod): Option[RunLineMarkerContributor.Info] = {

    def findParentThatImplementsFramework: (Option[PsiClass], Option[TestFramework]) = {
      val parent = Option(PsiTreeUtil.getParentOfType(method, classOf[PsiClass]))
      lazy val topmostParent = Option(PsiTreeUtil.getTopmostParentOfType(method, classOf[PsiClass]))

      parent.flatMap(parent => Option(TestFrameworks.detectFramework(parent))) match {
        case framework@Some(_) => (parent, framework)
        case _ =>
          (topmostParent, topmostParent.flatMap(parent => Option(TestFrameworks.detectFramework(parent))))
      }
    }

    val (parent, framework) = findParentThatImplementsFramework

    (parent, framework, method) match {
      case (_, Some(_: ScalaTestTestFramework), f: ScFunctionDefinition) => infoForScalaTestRefSpec(f)
      case (Some(clazz), Some(f), _) if f.isTestMethod(method) =>
        Some(buildLineInfo(
          s"java:test://${clazz.qualifiedName}.${method.getName}",
          method.getProject,
          isClass = false
        ))
      case _ => None
    }
  }

  private def infoForTestMethodRef(ref: ScReferenceExpression): Option[RunLineMarkerContributor.Info] = {
    val definition = PsiTreeUtil.getParentOfType(ref, classOf[ScTypeDefinition])
    if (definition == null)
      return None

    // NOT: for MUnit more than one framework is applicable: MUnit and JUnit, and JUnit is detected first =(
    val frameworks = TestFrameworks.detectApplicableFrameworks(definition).asScala
    val scalaFramework = frameworks.filterByType[AbstractTestFramework].headOption

    scalaFramework.flatMap {
      case _: ScalaTestTestFramework => infoForScalaTestMethodRef(ref, definition)
      case _: MUnitTestFrameworkMarker => infoForMUnitMethodRef(ref, definition)
      case _: Specs2TestFramework => infoForSpecs2MethodRef(ref, definition)
      case _ => None
    }
  }

  private def infoForSpecs2MethodRef(ref: ScReferenceExpression, definition: ScTypeDefinition): Option[RunLineMarkerContributor.Info] = {
    val locations = Specs2TestLocationsFinder.calculateTestLocations(definition)
    if (locations.contains(ref))
      Some(buildLineInfo(url = "", ref.getProject, isClass = false))
    else None
  }

  private def infoForScalaTestRefSpec(functionOrObject: ScalaPsiElement): Option[RunLineMarkerContributor.Info] = {
    val definition = PsiTreeUtil.getParentOfType(functionOrObject, classOf[ScClass])

    if (definition == null)
      return None

    val frameworks = TestFrameworks.detectApplicableFrameworks(definition).asScala
    val scalaFramework = frameworks.findByType[ScalaTestTestFramework]

    scalaFramework.flatMap { _ =>
      functionOrObject match {
        case f: ScFunctionDefinition => infoForScalaTestRefSpecFunctionDefinition(f, definition)
        case o: ScObject => infoForScalaTestRefSpecObject(o, definition)
        case _ => None
      }
    }
  }

  private def infoForScalaTestMethodRef(ref: ScReferenceExpression, definition: ScTypeDefinition): Option[RunLineMarkerContributor.Info] = {
    val locations = ScalaTestTestLocationsFinder.calculateTestLocations(definition)
    if (locations.contains(ref)) {
      val url = scalaTestLineUrl(ref, definition, testName = "")
      val info = buildLineInfo(url, ref.getProject, isClass = false)
      Some(info)
    }
    else None
  }

  private def infoForScalaTestRefSpecFunctionDefinition(functionDefinition: ScFunctionDefinition, definition: ScTypeDefinition): Option[RunLineMarkerContributor.Info] = {
    val locations = ScalaTestTestLocationsFinder.calculateTestLocations(definition)
    if (locations.contains(functionDefinition)) {
      val url = scalaTestLineUrl(functionDefinition, definition, testName = "")
      val info = buildLineInfo(url, functionDefinition.getProject, isClass = false)
      Some(info)
    }
    else None
  }

  private def infoForScalaTestRefSpecObject(obj: ScObject, definition: ScTypeDefinition): Option[RunLineMarkerContributor.Info] = {
    val locations = ScalaTestTestLocationsFinder.calculateTestLocations(definition)
    if (locations.contains(obj))
      Some(buildLineInfo("", obj.getProject, isClass = false))
    else None
  }

  private def infoForMUnitMethodRef(ref: ScReferenceExpression, definition: ScTypeDefinition): Option[RunLineMarkerContributor.Info] = {
    val locations = MUnitTestLocationsFinder.calculateTestLocations(definition)
    if (locations.exists(_.contains(ref))) {
      val testName = MUnitUtils.staticTestName(ref)
      val url = testName.fold("")(junit4TestMethodUrl(definition.qualifiedName, _))
      val info = buildLineInfo(url, ref.getProject, isClass = false)
      Some(info)
    }
    else None
  }

  private def junit4TestMethodUrl(classFqn: String, testName: String) =
    s"java:test://$classFqn/$testName"

  private def scalaTestLineUrl(element: PsiElement, definition: ScTypeDefinition, testName: String): String = {
    // NOTE: there are difficulties with identifying test by test lines due to when test file is modified lines
    // can shift and some url which used to refer to some failing test can suddenly refer to some new test and
    // a failing icon will be shown even if the test is successfully passed

    //val className  = definition.qualifiedName
    //val file       = definition.getContainingFile
    //val fileName   = file.getName
    //val document   = PsiDocumentManager.getInstance(project).getDocument(file)
    //val lineNumber = document.getLineNumber(element.startOffset) + 1
    //s"scalatest://LineInFile:$className:$fileName:${lineNumber}TestName:$testName"

    ""
  }

  protected def buildLineInfo(url: String, project: Project, isClass: Boolean): RunLineMarkerContributor.Info = {
    val icon = iconFor(url, project, isClass)
    val actions = ExecutorAction.getActions(1)
    new RunLineMarkerContributor.Info(icon, (_: PsiElement) => TestingSupportBundle.message("scalatest.gutter.run.test"), actions*)
  }

  private def iconFor(url: String, project: Project, isClass: Boolean): Image = {
    import Magnitude._

    def defaultIcon =
      if (isClass) ExecutionIconGroup.gutterRerun()
      else ExecutionIconGroup.gutterRun()

    val testState = Option(TestStateStorage.getInstance(project).getState(url))
    val testMagnitude = testState.map(state => TestIconMapper.getMagnitude(state.magnitude))

    testMagnitude.fold(defaultIcon) {
      case ERROR_INDEX | FAILED_INDEX => ExecutionIconGroup.gutterRunerror()
      case PASSED_INDEX | COMPLETE_INDEX => ExecutionIconGroup.gutterRunsuccess()
      case _ => defaultIcon
    }
  }
}
