package org.jetbrains.plugins.scala.annotator.hints

import consulo.codeEditor.EditorFactory
import consulo.document.FileDocumentManager
import consulo.project.Project
import consulo.util.dataholder.Key
import consulo.language.psi.{PsiElement, PsiManager}
import consulo.language.file.light.LightVirtualFile
import org.jetbrains.plugins.scala.annotator.hints.AnnotatorHints.AnnotatorHintsKey
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode

// Annotator hints, SCL-15593
case class AnnotatorHints(hints: Seq[Hint], modificationCount: Long) {
  def putTo(element: PsiElement): Unit = {
    val showCompilerErrors =
      Option(element.getContainingFile).exists(ScalaHighlightingMode.isShowErrorsFromCompilerEnabled)
    if (!showCompilerErrors)
      element.putUserData(AnnotatorHintsKey, this)
  }
}

object AnnotatorHints {
  private val AnnotatorHintsKey = Key.create[AnnotatorHints]("AnnotatorHints")

  def in(element: PsiElement): Option[AnnotatorHints] = Option(element.getUserData(AnnotatorHintsKey))

  def clearIn(element: PsiElement): Unit = {
    element.putUserData(AnnotatorHintsKey, null)
  }

  //noinspection InstanceOf
  def clearIn(project: Project): Unit =
    for {
      editor <- EditorFactory.getInstance().getAllEditors
      if editor.getProject == project
      file <- Option(FileDocumentManager.getInstance().getFile(editor.getDocument))
      if !file.isInstanceOf[LightVirtualFile]
      psiFile <- Option(PsiManager.getInstance(project).findFile(file))
    } {
      psiFile.elements.foreach(clearIn)
    }
}