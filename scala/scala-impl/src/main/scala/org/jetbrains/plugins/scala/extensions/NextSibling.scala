package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement

object NextSibling {
  def unapply(e: PsiElement): Option[PsiElement] = Option(e.getNextSibling)
}