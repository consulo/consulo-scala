package org.jetbrains.plugins.scala.lang.psi.impl.base
package types

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.base.types._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl

class ScSequenceArgImpl(node: ASTNode) extends ScalaPsiElementImpl (node) with ScSequenceArg{
  override def toString: String = "SequenceArgumentType"
}