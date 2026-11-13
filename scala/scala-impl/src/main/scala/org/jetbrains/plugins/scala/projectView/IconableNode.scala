package org.jetbrains.plugins.scala.projectView

import consulo.ui.ex.tree.PresentationData
import consulo.ui.ex.awt.tree.ValidateableNode
import consulo.component.util.Iconable

import java.util.Collections.emptyList

private[projectView] trait IconableNode extends ValidateableNode with Iconable {

  //noinspection TypeAnnotation
  final def emptyNodesList = emptyList[Node]()

  final def setIcon(data: PresentationData): Unit = {
    data.setIcon(getIcon(0))
  }
}
