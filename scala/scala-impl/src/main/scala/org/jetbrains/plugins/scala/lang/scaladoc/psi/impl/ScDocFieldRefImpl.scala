package org.jetbrains.plugins.scala.lang.scaladoc.psi.impl

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocFieldRef

class ScDocFieldRefImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScDocFieldRef{
  override def toString: String = "DocFieldRef"
}