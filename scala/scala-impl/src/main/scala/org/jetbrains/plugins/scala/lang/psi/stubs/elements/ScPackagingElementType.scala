package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.packaging.ScPackagingImpl

final class ScPackagingElementType extends ScStubElementType[ScPackaging]("packaging") {
  override def createElement(node: ASTNode): ScPackaging = new ScPackagingImpl(node)
}
