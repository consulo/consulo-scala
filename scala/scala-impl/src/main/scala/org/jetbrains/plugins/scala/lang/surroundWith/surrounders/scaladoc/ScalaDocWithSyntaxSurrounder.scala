package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.scaladoc

import consulo.codeEditor.Editor
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createScalaDocSimpleData
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType
import org.jetbrains.plugins.scala.lang.surroundWith.ScalaPsiSurrounder

trait ScalaDocWithSyntaxSurrounder extends ScalaPsiSurrounder {
  override def isApplicable(elements: Array[PsiElement]): Boolean = elements != null && elements.nonEmpty

  override def surroundElements(elements: Array[PsiElement], editor: Editor): Option[TextRange] = {
    val selection = editor.getSelectionModel
    val startOffset = selection.getSelectionStart
    val endOffset = selection.getSelectionEnd

    val element = elements(0)
    val offset = element.getTextOffset

    def getNewExprText(expr: String): String = expr.substring(0, startOffset - offset) + getSyntaxTag +
      expr.substring(startOffset - offset, endOffset - offset) + getSyntaxTag + expr.substring(endOffset - offset)

    val surroundedText = new StringBuilder()
    elements.foreach(surroundedText append _.getText)

    var newExpr = createScalaDocSimpleData(getNewExprText(surroundedText.toString()))(using element.getManager)

    while (newExpr != null && newExpr.getNode.getElementType != ScalaDocTokenType.DOC_COMMENT_END) {
      element.getParent.addBefore(newExpr, element)
      newExpr = newExpr.getNextSibling
    }

    elements.foreach(_.delete())

    val range = new TextRange(endOffset + 2 * getSyntaxTag.length, endOffset + 2 * getSyntaxTag.length)
    Some(range)
  }

  def getSyntaxTag: String
}
