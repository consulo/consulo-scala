package org.jetbrains.plugins.scala.lang.psi.impl.expr

import consulo.language.ast.ASTNode
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.types.result.TypeResult

class ScPostfixExprImpl(node: ASTNode) extends MethodInvocationImpl(node) with ScPostfixExpr {

  override def argumentExpressions: Seq[ScExpression] = Seq.empty

  override def getInvokedExpr: ScExpression = operation

  override def argsElement: PsiElement = operation

  override def toString: String = "PostfixExpression"

  override protected def innerType: TypeResult = getEffectiveInvokedExpr.getNonValueType()
}