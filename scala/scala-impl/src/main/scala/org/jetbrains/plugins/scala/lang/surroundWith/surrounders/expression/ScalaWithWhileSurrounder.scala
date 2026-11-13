package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.localize.LocalizeValue

import consulo.language.ast.ASTNode
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScWhile

/*
 * Surrounds expression with while: while { <Cursor> } { Expression }
 */
class ScalaWithWhileSurrounder extends ScalaExpressionSurrounder {
  override def getTemplateAsString(elements: Array[PsiElement]): String =
    "while (true) {" + super.getTemplateAsString(elements) + "}"

  //noinspection ScalaExtractStringToBundle,DialogTitleCapitalization
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of("while")

  override protected val isApplicableToMultipleElements: Boolean = true

  override def getSurroundSelectionRange(withWhileNode: ASTNode): Option[TextRange] =
    unwrapParenthesis(withWhileNode) match {
      case Some(stmt: ScWhile) =>
        val whileStmt = stmt.toIndentationBasedSyntax
        whileStmt.condition.map(_.getTextRange)
      case _ => None
    }
}
