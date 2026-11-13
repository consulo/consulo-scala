package org.jetbrains.plugins.scala.editor.selectioner

import consulo.language.editor.action.ExtendWordSelectionHandlerBase
import consulo.language.ast.ASTNode
import consulo.codeEditor.Editor
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScBlockStatement

import java.util

/**
  * Selects a statement together with its trailing semicolon.
 */
class ScalaSemicolonSelectioner extends ExtendWordSelectionHandlerBase {
  override def canSelect(e: PsiElement): Boolean = e.isInstanceOf[ScBlockStatement]

  override def select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): util.ArrayList[TextRange] = {
    val treeNext: ASTNode = e.getNode.getTreeNext
    val result = new util.ArrayList[TextRange]
    if (treeNext != null && treeNext.getElementType == ScalaTokenTypes.tSEMICOLON) {
      val r = new TextRange(e.getTextRange.getStartOffset, treeNext.getTextRange.getEndOffset)
      result.add(r)
    }
    result
  }
}