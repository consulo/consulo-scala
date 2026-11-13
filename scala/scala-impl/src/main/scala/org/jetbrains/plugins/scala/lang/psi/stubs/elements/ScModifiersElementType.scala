package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.base.ScModifierList
import org.jetbrains.plugins.scala.lang.psi.impl.base.ScModifierListImpl

final class ScModifiersElementType extends ScStubElementType[ScModifierList]("modifiers") {
  override def createElement(node: ASTNode): org.jetbrains.plugins.scala.lang.psi.impl.base.ScModifierListImpl = new ScModifierListImpl(node)
}
