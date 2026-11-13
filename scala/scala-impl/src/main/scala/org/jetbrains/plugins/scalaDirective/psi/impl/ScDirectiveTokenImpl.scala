package org.jetbrains.plugins.scalaDirective.psi.impl

import consulo.language.impl.psi.LeafPsiElement
import org.jetbrains.plugins.scalaDirective.lang.lexer.ScalaDirectiveElementType
import org.jetbrains.plugins.scalaDirective.psi.api.ScDirectiveToken

class ScDirectiveTokenImpl(
  override val tokenType: ScalaDirectiveElementType,
  text: CharSequence
) extends LeafPsiElement(tokenType, text) with ScDirectiveToken {

  override def toString: String = s"ScDirectiveToken(${tokenType.toString})"

}
