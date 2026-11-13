package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement

object PrevElement {
  def unapply(e: PsiElement): Option[PsiElement] = e.prevElement
}
