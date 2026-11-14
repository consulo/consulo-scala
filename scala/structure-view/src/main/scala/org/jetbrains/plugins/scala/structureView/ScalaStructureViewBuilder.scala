package org.jetbrains.plugins.scala.structureView

import com.intellij.ide.structureView.{StructureViewModel, TreeBasedStructureViewBuilder}
import consulo.codeEditor.Editor
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

class ScalaStructureViewBuilder(file: ScalaFile) extends TreeBasedStructureViewBuilder {
  override def createStructureViewModel(editor: Editor): StructureViewModel = new ScalaStructureViewModel(file)

  override def isRootNodeShown: Boolean = false
}
