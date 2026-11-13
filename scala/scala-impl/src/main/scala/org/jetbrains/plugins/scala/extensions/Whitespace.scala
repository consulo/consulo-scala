package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.{PsiElement, PsiWhiteSpace}

object Whitespace {
  def unapply(e: PsiElement): Option[String] = Some(e) collect {
    case _: PsiWhiteSpace => e.getText
  }
}
