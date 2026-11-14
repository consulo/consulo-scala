package org.jetbrains.plugins.scala.testingSupport

import consulo.annotation.component.ExtensionImpl

import consulo.fileEditor.structureView.tree.NodeProvider
import consulo.fileEditor.structureView.tree.{TreeElement}
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.structureView.ScalaStructureViewModelProvider
import org.jetbrains.plugins.scala.testingSupport.test.structureView.TestNodeProvider

//noinspection ApiStatus
@ExtensionImpl
private final class TestStructureViewModelProvider extends ScalaStructureViewModelProvider {
  override def nodeProvidersFor(rootElement: ScalaFile): Seq[NodeProvider[? <: TreeElement]] = {
    if (rootElement.getFileType == ScalaFileType.INSTANCE)
      Seq(new TestNodeProvider())
    else
      Seq.empty
  }
}
