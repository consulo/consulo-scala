package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement

object PrevSibling {
  def unapply(e: PsiElement): Option[PsiElement] = Option(e.getPrevSibling)
}