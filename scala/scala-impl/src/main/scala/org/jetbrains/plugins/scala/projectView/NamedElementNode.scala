package org.jetbrains.plugins.scala.projectView

import consulo.ui.image.Image
import consulo.project.ui.view.tree.AbstractPsiBasedNode
import consulo.ui.ex.tree.PresentationData
import consulo.project.ui.view.tree.ViewSettings
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.project.ProjectContext

import java.util
import java.util.Collections

private class NamedElementNode(element: ScNamedElement)(implicit project: ProjectContext, settings: ViewSettings)
  extends AbstractPsiBasedNode[ScNamedElement](project, element, settings) with IconableNode {

  override protected def extractPsiFromValue: PsiElement = getValue

  override protected def getChildrenImpl: util.Collection[Node] = Collections.emptyList()

  override protected def updateImpl(data: PresentationData): Unit =
    Option(getValue).filter(_.isValid).foreach(it => data.setPresentableText(it.name))

  override def getIcon(flags: Int): Image = element.getIcon(flags)
}