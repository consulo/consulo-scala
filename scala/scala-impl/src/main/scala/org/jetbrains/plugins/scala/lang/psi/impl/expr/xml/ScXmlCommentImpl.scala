package org.jetbrains.plugins.scala.lang.psi.impl.expr
package xml

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.expr.xml._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl

class ScXmlCommentImpl(node: ASTNode) extends ScalaPsiElementImpl (node) with ScXmlComment{
  override def toString: String = "XmlComment"
}