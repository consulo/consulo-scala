package org.jetbrains.plugins.scala.lang.scaladoc.psi.impl

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocInnerCodeElement

class ScDocInnerCodeElementImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScDocInnerCodeElement {
  override def toString = "InnerCodeElement"
}