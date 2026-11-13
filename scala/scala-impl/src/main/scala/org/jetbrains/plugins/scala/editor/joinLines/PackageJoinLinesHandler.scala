package org.jetbrains.plugins.scala.editor.joinLines

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.action.{JoinLinesHandlerDelegate, JoinRawLinesHandlerDelegate}
import consulo.document.Document
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.editor.DocumentExt
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging

/** `package a\npackageb` => `package a.b` */
@ExtensionImpl
class PackageJoinLinesHandler extends JoinRawLinesHandlerDelegate {

  override def tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int = {
    val elementAtStart: PsiElement = file.findElementAt(start)
    val elementAtEnd: PsiElement = file.findElementAt(end)

    if (elementAtEnd == null || elementAtStart == null)
      return JoinLinesHandlerDelegate.CANNOT_JOIN

    (elementAtStart.getParent, elementAtEnd.getParent) match {
      case (p1: ScPackaging, p2: ScPackaging) if p2.getParent == p1 =>
        val nextLineStart = document.lineStartOffset(end)
        val nextLineEnd = (document.lineEndOffset(end) + 1).min(document.getTextLength)
        document.deleteString(nextLineStart, nextLineEnd)

        val insertIdx = p1.reference.map(_.getTextRange.getEndOffset).getOrElse(p1.getTextRange.getStartOffset)
        document.insertString(insertIdx, s".${p2.packageName}")
        insertIdx
      case _ =>
        JoinLinesHandlerDelegate.CANNOT_JOIN
    }
  }

  override def tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int): Int = {
    JoinLinesHandlerDelegate.CANNOT_JOIN
  }

}
