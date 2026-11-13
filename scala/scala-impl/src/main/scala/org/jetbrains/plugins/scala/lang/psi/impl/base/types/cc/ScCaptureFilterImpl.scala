package org.jetbrains.plugins.scala.lang.psi.impl.base.types.cc

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.base.types.cc.ScCaptureFilter
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl

class ScCaptureFilterImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScCaptureFilter {
  override def toString: String = "CaptureFilter"
}
