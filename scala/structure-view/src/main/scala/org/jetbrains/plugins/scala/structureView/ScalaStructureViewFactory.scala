package org.jetbrains.plugins.scala.structureView

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.fileEditor.structureView.StructureViewBuilder
import consulo.language.editor.structureView.PsiStructureViewFactory
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.statistics.ScalaActionUsagesCollector

@ExtensionImpl
class ScalaStructureViewFactory extends PsiStructureViewFactory {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def getStructureViewBuilder(file: PsiFile): StructureViewBuilder = file match {
    case scalaFile: ScalaFile =>
      ScalaActionUsagesCollector.logStructureView(file.getProject)
      new ScalaStructureViewBuilder(scalaFile)
    case _ => null
  }
}
