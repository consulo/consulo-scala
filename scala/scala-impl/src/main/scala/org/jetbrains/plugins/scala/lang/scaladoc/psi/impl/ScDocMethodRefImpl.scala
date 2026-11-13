package org.jetbrains.plugins.scala.lang.scaladoc.psi.impl

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocMethodRef

class ScDocMethodRefImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScDocMethodRef{
  override def toString: String = "DocMethodRef"
}