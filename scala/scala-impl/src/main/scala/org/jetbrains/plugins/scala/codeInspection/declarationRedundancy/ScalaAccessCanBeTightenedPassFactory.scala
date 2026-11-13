package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.language.editor.Pass
import consulo.language.editor.highlight.{TextEditorHighlightingPass, TextEditorHighlightingPassFactory}
import com.intellij.codeHighlighting.{TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassRegistrar}
import consulo.language.editor.FileStatusMap
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

final class ScalaAccessCanBeTightenedPassFactory
  extends TextEditorHighlightingPassFactory
    with TextEditorHighlightingPassFactoryRegistrar {

  override def createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass = {
    //optimization similar to com.intellij.codeInsight.daemon.impl.GeneralHighlightingPassFactory.createHighlightingPass
    val dirtyRange = FileStatusMap.getDirtyTextRange(editor.getDocument, file, Pass.UPDATE_ALL)
    val nothingChangedInFile = dirtyRange == null
    if (nothingChangedInFile)
      null
    else file match {
      case scalaFile: ScalaFile =>
        new ScalaAccessCanBeTightenedPass(scalaFile, Option(editor.getDocument))
      case _ =>
        null
    }
  }

  override def registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project): Unit = {
    registrar.registerTextEditorHighlightingPass(this, Array[Int](Pass.UPDATE_ALL), null, false, -1)
  }
}
