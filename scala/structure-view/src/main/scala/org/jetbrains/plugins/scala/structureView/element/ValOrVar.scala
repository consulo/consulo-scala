package org.jetbrains.plugins.scala.structureView.element

import consulo.component.util.Iconable
import consulo.language.icon.IconDescriptorUpdaters
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.{IteratorExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScBlockExpr
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScPatternDefinition, ScValue, ScValueOrVariable, ScVariable, ScVariableDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.types.TypePresentationContext
import org.jetbrains.plugins.scala.structureView.element.AbstractItemPresentation.withSimpleNames

import consulo.ui.image.Image

abstract class ValOrVar(element: ScNamedElement, private[structureView] val parent: ScValueOrVariable, inherited: Boolean)
  extends AbstractTreeElementDelegatingChildrenToPsi(element, inherited)
    with InheritedLocationStringItemPresentation {

  override def location: Option[String] = value.map(_.containingClass).map(_.name)

  override def presentableText: String = {
    implicit val tpc: TypePresentationContext = TypePresentationContext(element)
    val typeAnnotation = value.flatMap(_.typeElement.map(_.getText))
    withSimpleNames(element.name + typeAnnotation.map(": " + _).mkString)
  }

  override def getIcon: Image =
    value.map(IconDescriptorUpdaters.getIcon(_, Iconable.ICON_FLAG_VISIBILITY)).orNull

  override def children: Seq[PsiElement] = {
    val definition = value match {
      case Some(definition: ScPatternDefinition)  => definition.expr
      case Some(definition: ScVariableDefinition) => definition.expr
      case _                                      => None
    }
    definition match {
      case Some(block: ScBlockExpr) => Block.childrenOf(block, includeLocalValOrVarDefinitions = true)
      case _                        => Seq.empty
    }
  }

  override def isAlwaysLeaf: Boolean = false

  override def getAccessLevel: Int = getAccessLevel(parent)

  private def value: Option[ScValueOrVariable] = element.parentsInFile.findByType[ScValueOrVariable]
}

final class Value(element: ScNamedElement, parent: ScValue, inherited: Boolean) extends ValOrVar(element, parent, inherited)
final class Variable(element: ScNamedElement, parent: ScVariable, inherited: Boolean) extends ValOrVar(element, parent, inherited)
