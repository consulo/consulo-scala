package org.jetbrains.plugins.scala.projectView

import com.intellij.java.impl.ide.projectView.impl.nodes.ClassTreeNode
import consulo.ui.ex.tree.PresentationData
import consulo.project.ui.view.tree.ViewSettings
import consulo.project.Project
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition

private[projectView] abstract class CustomDefinitionNode(definition: ScTypeDefinition)
                                                        (implicit project: Project, settings: ViewSettings)
  extends ClassTreeNode(project, definition, settings) with IconableNode {

  myName = definition.name

  override def updateImpl(data: PresentationData): Unit = validValue match {
    case Some(definition) => data.setPresentableText(definition.name)
    case _ => super.updateImpl(data)
  }

  protected final def validValue: Option[ScTypeDefinition] = getValue match {
    case definition: ScTypeDefinition if definition.isValid => Some(definition)
    case _ => None
  }
}
