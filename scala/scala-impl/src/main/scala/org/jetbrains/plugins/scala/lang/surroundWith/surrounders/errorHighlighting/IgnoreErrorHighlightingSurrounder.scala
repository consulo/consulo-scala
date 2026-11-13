package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.errorHighlighting

import consulo.localize.LocalizeValue

import consulo.codeEditor.Editor
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.surroundWith.ScalaPsiSurrounder

class IgnoreErrorHighlightingSurrounder extends ScalaPsiSurrounder {
  //noinspection ScalaExtractStringToBundle
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of("/*_*/.../*_*/")

  override def isApplicable(elements: Array[PsiElement]): Boolean = elements.nonEmpty

  override def surroundElements(elements: Array[PsiElement], editor: Editor): Option[TextRange] = {
    val firstElement = elements.head
    firstElement.containingFile.map { file =>
      val text = "/*_*/"
      val start = firstElement.startOffset
      val end = elements.last.endOffset

      val document = file.getFileDocument
      document.insertString(end, text)
      document.insertString(start, text)

      new TextRange(end + 2 * text.length, end + 2 * text.length)
    }
  }
}
