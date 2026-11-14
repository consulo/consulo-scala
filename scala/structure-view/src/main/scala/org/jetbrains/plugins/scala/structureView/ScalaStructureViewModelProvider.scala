package org.jetbrains.plugins.scala.structureView

import com.intellij.ide.util.treeView.smartTree.{NodeProvider, TreeElement}
import org.jetbrains.plugins.scala.ExtensionPointDeclaration
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

// upstream: @ApiStatus.Internal
trait ScalaStructureViewModelProvider {
  def nodeProvidersFor(rootElement: ScalaFile): Seq[NodeProvider[? <: TreeElement]]
}

object ScalaStructureViewModelProvider extends ExtensionPointDeclaration[ScalaStructureViewModelProvider](
  "org.intellij.scala.structureViewModelProvider") {

  def nodeProvidersFor(rootElement: ScalaFile): Seq[NodeProvider[? <: TreeElement]] =
    implementations.flatMap(_.nodeProvidersFor(rootElement))
}
