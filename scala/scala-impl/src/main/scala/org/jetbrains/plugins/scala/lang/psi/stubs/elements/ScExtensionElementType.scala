package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScExtension
import org.jetbrains.plugins.scala.lang.psi.impl.statements.ScExtensionImpl

final class ScExtensionElementType extends ScStubElementType[ScExtension]("extension") {
  override def createElement(node: ASTNode): ScExtension = new ScExtensionImpl(null, node)
}
