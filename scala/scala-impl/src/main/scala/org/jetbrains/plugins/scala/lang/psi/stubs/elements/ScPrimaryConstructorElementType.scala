package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor
import org.jetbrains.plugins.scala.lang.psi.impl.base.ScPrimaryConstructorImpl

final class ScPrimaryConstructorElementType extends ScStubElementType[ScPrimaryConstructor]("primary constructor") {
  override def createElement(node: ASTNode): ScPrimaryConstructor = new ScPrimaryConstructorImpl(node)
}
