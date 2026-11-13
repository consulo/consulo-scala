package org.jetbrains.plugins.scalaDirective.psi.api

import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scalaDirective.lang.lexer.ScalaDirectiveElementType

trait ScDirectiveToken extends PsiElement {
  def tokenType: ScalaDirectiveElementType
}

