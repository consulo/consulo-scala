package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement
import consulo.language.impl.psi.LeafPsiElement
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType

object DocWhitespace {
  def unapply(e: PsiElement): Option[String] = e match {
    case leaf: LeafPsiElement if leaf.getElementType == ScalaDocTokenType.DOC_WHITESPACE => Some(leaf.getText)
    case _ => None
  }
}
