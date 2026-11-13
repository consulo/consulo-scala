package org.jetbrains.plugins.scala.lang.psi.stubs.elements.signatures

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameters
import org.jetbrains.plugins.scala.lang.psi.impl.statements.params.ScParametersImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScStubElementType

final class ScParamClausesElementType extends ScStubElementType[ScParameters]("parameter clauses") {
  override def createElement(node: ASTNode): ScParameters = new ScParametersImpl(node)
}
