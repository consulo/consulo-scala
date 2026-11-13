package org.jetbrains.plugins.scala.codeInspection.quickfix

import org.jetbrains.plugins.scala.codeInspection.{PsiUpdateContext, PsiUpdateFix}
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

final class RemoveExpressionQuickFix(expression: ScExpression) extends PsiUpdateFix[ScExpression](expression) {
  override def getFamilyName: String = ScalaInspectionBundle.message("remove.expression")

  override def invoke(context: PsiUpdateContext, expression: ScExpression): Unit =
    expression.delete()
}
