package org.jetbrains.plugins.scala.lang.psi.stubs.elements.signatures

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.impl.statements.params.ScClassParameterImpl

final class ScClassParameterElementType extends ScParamElementType[ScClassParameter]("class parameter") {
  override def createElement(node: ASTNode): ScClassParameter = new ScClassParameterImpl(node)
}
