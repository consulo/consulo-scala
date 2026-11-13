package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.localize.LocalizeValue

import consulo.language.ast.ASTNode
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement

/*
 * ScalaWithBracketsSurrounder is responsible of surrounders, witch enclause expression in brackets: { Expression } or ( Expression )
 */
class ScalaWithBracesSurrounder extends ScalaExpressionSurrounder {

  override def getTemplateAsString(elements: Array[PsiElement]): String = "{" + super.getTemplateAsString(elements) + "}"

  //noinspection ScalaExtractStringToBundle
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of("{  }")

  override def getSurroundSelectionRange(expr: ASTNode): Option[TextRange] = {
    val offset = expr.getTextRange.getEndOffset
    val range = new TextRange(offset, offset)
    Some(range)
  }

  override def needParenthesis(element: PsiElement) = false

  override protected val isApplicableToMultipleElements: Boolean = true
}
