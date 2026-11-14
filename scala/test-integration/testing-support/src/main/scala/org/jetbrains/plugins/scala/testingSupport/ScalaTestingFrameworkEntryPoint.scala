package org.jetbrains.plugins.scala.testingSupport

import consulo.annotation.component.ExtensionImpl

import com.intellij.java.language.codeInsight.TestFrameworks
import com.intellij.java.analysis.codeInspection.ex.EntryPoint
import consulo.language.editor.inspection.reference.RefElement
import consulo.language.psi.PsiElement
import consulo.language.psi.util.PsiTreeUtil
import consulo.util.xml.serializer.XmlSerializer
import org.jdom.Element
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject, ScTemplateDefinition}
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestTestLocationsFinder

import scala.beans.BooleanBeanProperty

@ExtensionImpl
class ScalaTestingFrameworkEntryPoint extends EntryPoint {

  @BooleanBeanProperty
  var selected: Boolean = true

  override def getDisplayName: String = TestingSupportBundle.message("scala.test.entry.points")

  override def isEntryPoint(refElement: RefElement, psiElement: PsiElement): Boolean =
    isEntryPoint(psiElement)

  override def isEntryPoint(psiElement: PsiElement): Boolean = {

    def isTestClass(clazz: ScTemplateDefinition): Boolean = {
      val framework = TestFrameworks.detectFramework(clazz)
      framework != null && framework.isTestClass(clazz)
    }

    def isScalaTestRefSpecMethodOrObject(element: PsiElement): Boolean =
      Option(PsiTreeUtil.getTopmostParentOfType(element, classOf[ScClass])).exists { definition =>
        ScalaTestTestLocationsFinder.calculateTestLocations(definition).contains(element)
      }

    psiElement match {
      case templateDefinition: ScTemplateDefinition if isTestClass(templateDefinition) => true

      case methodOrObject@(_: ScFunctionDefinition | _: ScObject) =>
        Option(PsiTreeUtil.getParentOfType(psiElement, classOf[ScClass]))
          .flatMap(scClass => Option(TestFrameworks.detectFramework(scClass)))
          .exists { _ => isScalaTestRefSpecMethodOrObject(methodOrObject) }

      case _ => false
    }
  }

  override def readExternal(element: Element): Unit =
    XmlSerializer.deserializeInto(this, element)

  override def writeExternal(element: Element): Unit =
    XmlSerializer.serializeInto(this, element)

}
