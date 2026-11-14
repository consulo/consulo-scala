package org.jetbrains.plugins.scala.codeInsight.implicits

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.Pass
import consulo.language.editor.highlight.{TextEditorHighlightingPass, TextEditorHighlightingPassFactory}
import consulo.codeEditor.Editor
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.codeInsight.hints.ScalaHintsSettings
import org.jetbrains.plugins.scala.extensions.{PsiFileExt, ViewProviderExt}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

@ExtensionImpl
class ImplicitHintsPassFactory extends TextEditorHighlightingPassFactory {

  override def register(registrar: TextEditorHighlightingPassFactory.Registrar): Unit = {
    val runAfterAnnotator = Array(Pass.UPDATE_ALL)

    registrar.registerTextEditorHighlightingPass(this, runAfterAnnotator, null, false, -1)
  }

  override def createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass = file match {
    case file: ScalaFile if !ImplicitHints.isUpToDate(editor, file) =>
      new ImplicitHintsPass(editor, file, new ScalaHintsSettings.CodeInsightSettingsAdapter)
    case file: PsiFile if file.getViewProvider.hasScalaPsi && !ImplicitHints.isUpToDate(editor, file) =>
      file.findAnyScalaFile.map (
        scalaFile => new ImplicitHintsPass(editor, scalaFile, new ScalaHintsSettings.CodeInsightSettingsAdapter)
      ).orNull
    case _ =>
      null
  }
}
