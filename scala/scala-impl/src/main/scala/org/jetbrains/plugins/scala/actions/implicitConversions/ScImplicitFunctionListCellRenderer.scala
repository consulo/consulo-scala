package org.jetbrains.plugins.scala.actions.implicitConversions

import consulo.language.editor.ui.PsiElementListCellRenderer
import consulo.colorScheme.TextAttributes
import consulo.language.psi.{PsiElement, PsiNamedElement}
import org.jetbrains.plugins.scala.actions.Parameters
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScMember
import org.jetbrains.plugins.scala.lang.psi.types.Context
import org.jetbrains.plugins.scala.lang.psi.types.api.presentation.TypeAnnotationRenderer.ParameterTypeDecorator
import org.jetbrains.plugins.scala.lang.psi.types.api.presentation._
import org.jetbrains.plugins.scala.lang.psi.types.result._

import java.awt.Font
import javax.swing._

private class ScImplicitFunctionListCellRenderer(actual: PsiNamedElement, place: PsiElement)
  extends PsiElementListCellRenderer[PsiNamedElement] {

  override protected def getRightCellRenderer(value: Any): DefaultListCellRenderer =
    value match {
      case e: ScMember if e.isSynthetic => super.getRightCellRenderer(e.syntheticNavigationElement)
      case _                            => super.getRightCellRenderer(value)
    }

  override def getNavigationItemAttributes(value: Any): TextAttributes = {
    val result = super.getNavigationItemAttributes(value)
    if (value == actual) {
      val attributes = Option(result).getOrElse(new TextAttributes())
      attributes.setFontType(Font.BOLD)
      return attributes
    }
    result
  }

  // upstream: overrides `getListCellRendererComponent(JList, Object, ...)`, which is not overridable from Scala here
  def parametersRenderer: ListCellRenderer[Parameters] =
    (list: JList[? <: Parameters], value: Parameters, index: Int, isSelected: Boolean, cellHasFocus: Boolean) =>
      getListCellRendererComponent(list.asInstanceOf[JList[PsiNamedElement]], value.newExpression, index, isSelected, cellHasFocus)

  override def getElementText(element: PsiNamedElement): String =
    element match {
      case method: ScFunction  => functionRenderer.render(method)
      case b: ScBindingPattern => b.name + ": " + typeRenderer.render(b.`type`().getOrAny)
      case _                   => element.name
    }

  private def functionRenderer = new FunctionRenderer(
    typeParamsRenderer = None,
    paramsRenderer,
    typeAnnotationRenderer,
    renderDefKeyword = false
  )

  private def typeAnnotationRenderer = new TypeAnnotationRenderer(
    typeRenderer,
    ParameterTypeDecorator.DecorateAll
  )

  private def typeRenderer: TypeRenderer =
    _.presentableText(using place, Context(place))

  private def paramRenderer = new ParameterRenderer(
    typeRenderer,
    ModifiersRenderer.SimpleText(TextEscaper.Html),
    typeAnnotationRenderer,
    textEscaper,
    withMemberModifiers = true,
    withAnnotations = true
  )

  private def paramsRenderer: ParametersRenderer = new ParametersRenderer(
    paramRenderer,
    shouldRenderImplicitModifier = true
  )

  private def textEscaper: TextEscaper = TextEscaper.Html

  override def getIconFlags: Int = 0

  override def getContainerText(element: PsiNamedElement, name: String): String = null //todo: add package name
}
