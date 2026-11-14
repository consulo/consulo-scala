package org.jetbrains.plugins.scala.structureView

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.application.Application
import consulo.fileEditor.structureView.tree.{NodeProvider, TreeElement}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

import scala.jdk.CollectionConverters.*

// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
trait ScalaStructureViewModelProvider {
  def nodeProvidersFor(rootElement: ScalaFile): Seq[NodeProvider[? <: TreeElement]]
}

object ScalaStructureViewModelProvider {

  def nodeProvidersFor(rootElement: ScalaFile): Seq[NodeProvider[? <: TreeElement]] =
    Application.get().getExtensionPoint(classOf[ScalaStructureViewModelProvider])
      .getExtensionList.asScala.toSeq
      .flatMap(_.nodeProvidersFor(rootElement))
}
