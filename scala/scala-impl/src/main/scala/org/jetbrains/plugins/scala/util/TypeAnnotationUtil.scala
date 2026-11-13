package org.jetbrains.plugins.scala.util

import consulo.ide.setting.ShowSettingsUtil
import consulo.project.Project
import consulo.language.psi.PsiElement
import consulo.ui.ex.awt.HyperlinkLabel
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInsight.intention.types.AddOrRemoveStrategy
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, invokeLater}
import org.jetbrains.plugins.scala.lang.formatting.settings.TypeAnnotationsPanelBase.TAB_TITLE
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.statements._
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings.ReturnTypeLevel.{ADD, BY_CODE_STYLE, REMOVE}
import org.jetbrains.plugins.scala.settings._
import org.jetbrains.plugins.scala.compilersettings.settings._
import org.jetbrains.plugins.scala.settings.annotations._

import javax.swing.event.HyperlinkEvent
import scala.jdk.CollectionConverters._

// TODO more refactoring needed
object TypeAnnotationUtil {
  // We shouldn't convert _typed pattern_ to _variable pattern_, because typed pattern differs from type annotation:
  // val v: String = new Object() - type mismatch
  // val Some(v: String) = Some(new Object()) - compiles OK
  // Thus, the presence of type affects the outcome, and such a type is not optional.
  private def getTypeElement(element: ScalaPsiElement): Option[ScTypeElement] = element match {
    case function: ScFunction => function.returnTypeElement
    case pattern: ScPatternDefinition => pattern.typeElement
    case variable: ScVariableDefinition => variable.typeElement
    case _ => None
  }

  def removeTypeAnnotationIfNeeded(
    element: ScalaPsiElement,
    state: ScalaApplicationSettings.ReturnTypeLevel = ScalaApplicationSettings.ReturnTypeLevel.BY_CODE_STYLE
  ): Unit = {
    state match {
      case ADD => //nothing
      case REMOVE =>
        val typeElementOpt = getTypeElement(element)
        typeElementOpt.foreach(AddOrRemoveStrategy.removeTypeAnnotation)
      case BY_CODE_STYLE =>
        val typeElementOpt = getTypeElement(element)
        typeElementOpt match {
          case Some(typeElement) =>
            val settings = ScalaTypeAnnotationSettings(element.getProject)
            val isTypeAnnotationRequired = settings.isTypeAnnotationRequiredFor(Declaration(element), Location(element), Some(Definition(element)))
            if (!isTypeAnnotationRequired) {
              AddOrRemoveStrategy.removeTypeAnnotation(typeElement)
            }
          case _ =>
        }
    }
  }

  def removeAllTypeAnnotationsIfNeeded(elements: Seq[PsiElement],
                                       state: ScalaApplicationSettings.ReturnTypeLevel = ScalaApplicationSettings.ReturnTypeLevel.BY_CODE_STYLE): Unit = {
    elements.foreach(_.depthFirst().foreach {
      case scalaPsiElement: ScalaPsiElement => removeTypeAnnotationIfNeeded(scalaPsiElement, state)
      case _ =>
    })
  }

  // TODO It's better to avoid global variables
  private var requestCountsToShow: Int = 0

  def showTypeAnnotationsSettings(project: Project): Unit = {
    requestCountsToShow += 1
    showWindowInvokeLater(project)
  }

  // upstream: found the configurable via ShowSettingsUtilImpl/ConfigurableVisitor and switched CodeStyleMainPanel to the
  // type annotations tab (impl classes in Consulo), here the code style page is opened with the tab title as a filter
  private def showWindowInvokeLater(project: Project): Unit = {
    invokeLater {
      if (requestCountsToShow > 0) { // show window only for the first request
        requestCountsToShow = 0
        ShowSettingsUtil.getInstance.showSettingsDialog(project, "preferences.sourceCode.Scala", TAB_TITLE)
      }
    }
  }

  def createTypeAnnotationsHLink(project: Project, @Nls msg: String): HyperlinkLabel = {
    val typeAnnotationsSettings: HyperlinkLabel = new HyperlinkLabel(msg)
    typeAnnotationsSettings.addHyperlinkListener((e: HyperlinkEvent) => {
      if (e.getEventType eq HyperlinkEvent.EventType.ACTIVATED) {
        showTypeAnnotationsSettings(project)
      }
    })

    typeAnnotationsSettings.setToolTipText(ScalaBundle.message("default.ta.tooltip"))
    typeAnnotationsSettings
  }
}
