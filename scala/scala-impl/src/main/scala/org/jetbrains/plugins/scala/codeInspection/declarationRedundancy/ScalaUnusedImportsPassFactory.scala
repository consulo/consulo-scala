package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.language.editor.impl.highlight.MainHighlightingPassFactory
import consulo.language.editor.Pass
import consulo.language.editor.highlight.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.{TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassRegistrar}
import consulo.language.editor.ProblemHighlightFilter
import consulo.language.editor.FileStatusMap
import consulo.language.editor.impl.highlight.HighlightInfoProcessor
import consulo.document.Document
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile

final class ScalaUnusedImportsPassFactory
  extends TextEditorHighlightingPassFactory
    with TextEditorHighlightingPassFactoryRegistrar
    with MainHighlightingPassFactory {

  override def registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project): Unit = {
    registrar.registerTextEditorHighlightingPass(
      this,
      Array(Pass.UPDATE_ALL),
      null,
      false,
      -1
    )
  }

  override def createHighlightingPass(file: PsiFile, editor: Editor): ScalaUnusedImportPass = {
    val dirtyRange = FileStatusMap.getDirtyTextRange(editor.getDocument, file, Pass.UPDATE_ALL)
    val nothingChangedInFile = dirtyRange == null && (ScalaUnusedImportPass.isUpToDate(file) || !ProblemHighlightFilter.shouldHighlightFile(file))
    if (nothingChangedInFile)
      null
    else
      new ScalaUnusedImportPass(file, editor, editor.getDocument, HighlightInfoProcessor.getEmpty)
  }

  override def createMainHighlightingPass(file: PsiFile, document: Document,
                                          highlightInfoProcessor: HighlightInfoProcessor): org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.ScalaUnusedImportPass =
    new ScalaUnusedImportPass(file, null, document, highlightInfoProcessor)
}
