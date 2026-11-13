package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateParents
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.templates.ScTemplateParentsImpl

final class ScTemplateParentsElementType extends ScStubElementType[ScTemplateParents]("template parents") {
  override def createElement(node: ASTNode): ScTemplateParents = new ScTemplateParentsImpl(node)
}
