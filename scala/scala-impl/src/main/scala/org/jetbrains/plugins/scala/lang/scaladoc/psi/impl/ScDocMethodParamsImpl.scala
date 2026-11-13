package org.jetbrains.plugins.scala.lang.scaladoc.psi.impl

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocMethodParams

class ScDocMethodParamsImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScDocMethodParams{
  override def toString: String = "DocMethodParams"
}