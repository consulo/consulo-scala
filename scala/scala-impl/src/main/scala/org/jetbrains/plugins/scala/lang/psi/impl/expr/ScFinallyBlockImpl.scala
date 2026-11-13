package org.jetbrains.plugins.scala.lang.psi.impl.expr

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScFinallyBlock

class ScFinallyBlockImpl(node: ASTNode) extends ScExpressionImplBase(node) with ScFinallyBlock {
  override def toString: String = "FinallyBlock"
}