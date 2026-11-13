package org.jetbrains.plugins.scala.lang.psi.impl.base
package patterns

import consulo.language.ast.ASTNode
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns._

final class ScConstructorPatternImpl(node: ASTNode) extends ScExtractorPatternImpl(node) with ScConstructorPattern {
  override def toString: String = "ConstructorPattern"

  override def ref: ScStableCodeReference = findChild[ScStableCodeReference].get

  override def subpatterns: Seq[ScPattern] = if (args != null) args.patterns else Seq.empty
}
