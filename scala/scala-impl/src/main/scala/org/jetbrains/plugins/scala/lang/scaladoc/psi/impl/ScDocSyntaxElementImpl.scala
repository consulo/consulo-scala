package org.jetbrains.plugins.scala.lang.scaladoc.psi.impl

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocSyntaxElement

class ScDocSyntaxElementImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScDocSyntaxElement{
  override def toString: String = "DocSyntaxElement " + getFlags

  override protected def acceptScala(visitor: ScalaElementVisitor): Unit =
    visitor.visitWikiSyntax(this)
}