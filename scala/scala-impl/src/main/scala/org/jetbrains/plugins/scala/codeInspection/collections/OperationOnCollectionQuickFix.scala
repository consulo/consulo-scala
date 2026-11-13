package org.jetbrains.plugins.scala.codeInspection.collections

import org.jetbrains.plugins.scala.codeInspection.{PsiUpdateContext, PsiUpdateFix}
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionFromText

class OperationOnCollectionQuickFix(
  @Nls override val getFamilyName: String,
  expression: ScExpression,
  replacementText: String,
) extends PsiUpdateFix[ScExpression](expression) {
  override def invoke(context: PsiUpdateContext, expression: ScExpression): Unit = {
    val replacement = createExpressionFromText(replacementText, expression)(using context.project)
    expression.replaceExpression(replacement, removeParenthesis = true)
  }
}

object OperationOnCollectionQuickFix {

  def apply(simplification: Simplification): OperationOnCollectionQuickFix = {
    val Simplification(toReplace, replacementText, hint, _) = simplification
    new OperationOnCollectionQuickFix(hint, toReplace.getElement, replacementText)
  }
}
