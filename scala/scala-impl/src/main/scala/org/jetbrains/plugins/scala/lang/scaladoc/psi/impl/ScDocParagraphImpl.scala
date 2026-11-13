package org.jetbrains.plugins.scala.lang.scaladoc.psi.impl

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocParagraph

class ScDocParagraphImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScDocParagraph {
  override def toString: String = "ScDocParagraph"
}