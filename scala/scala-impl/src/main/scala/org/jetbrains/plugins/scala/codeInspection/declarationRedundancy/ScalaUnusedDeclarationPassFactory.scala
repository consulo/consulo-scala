package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.language.editor.Pass
import consulo.language.editor.highlight.{TextEditorHighlightingPass, TextEditorHighlightingPassFactory}
import consulo.language.editor.DaemonCodeAnalyzer
import consulo.codeEditor.Editor
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

final class ScalaUnusedDeclarationPassFactory
  extends TextEditorHighlightingPassFactory {

  override def createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass = {
    //optimization similar to com.intellij.codeInsight.daemon.impl.GeneralHighlightingPassFactory.createHighlightingPass
    val dirtyRange = DaemonCodeAnalyzer.getInstance(file.getProject).getFileStatusMap.getFileDirtyScope(editor.getDocument, Pass.UPDATE_ALL)
    val nothingChangedInFile = dirtyRange == null
    if (nothingChangedInFile)
      null
    else file match {
      case scalaFile: ScalaFile =>
        new ScalaUnusedDeclarationPass(scalaFile, Option(editor.getDocument))
      case _ =>
        null
    }
  }

  override def register(registrar: TextEditorHighlightingPassFactory.Registrar): Unit = {
    registrar.registerTextEditorHighlightingPass(this, Array[Int](Pass.UPDATE_ALL), null, false, -1)
  }
}
