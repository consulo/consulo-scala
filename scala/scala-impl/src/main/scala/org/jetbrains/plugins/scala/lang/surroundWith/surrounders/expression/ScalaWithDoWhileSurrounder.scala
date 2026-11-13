package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.localize.LocalizeValue

import consulo.language.ast.ASTNode
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, ToNullSafe}
import org.jetbrains.plugins.scala.lang.psi.api.expr._

/*
 * Surrounds expression with do - while: do { Expression } while { <Cursor> }
 */
class ScalaWithDoWhileSurrounder extends ScalaExpressionSurrounder {

  // do-while is not available in Scala 3
  override def isApplicable(elements: Array[PsiElement]): Boolean =
    elements.nonEmpty && !elements.head.isInScala3File && super.isApplicable(elements)

  override def getTemplateAsString(elements: Array[PsiElement]): String =
    "do {" + super.getTemplateAsString(elements) + "} while (true)"

  //noinspection ScalaExtractStringToBundle,DialogTitleCapitalization
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of("do / while")

  override def getSurroundSelectionRange(withDoWhileNode: ASTNode): Option[TextRange] =
    unwrapParenthesis(withDoWhileNode) match {
      case Some(stmt: ScDo) =>
        val conditionNode = stmt.getNode
          .nullSafe
          .map(_.getLastChildNode)
          .map(_.getTreePrev)
        conditionNode.map(_.getTextRange).toOption
      case _ => None
    }

  override protected val isApplicableToMultipleElements: Boolean = true
}
