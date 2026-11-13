package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement

object Parent {
  def unapply(e: PsiElement): Option[PsiElement] = Option(e).map(_.getParent)
}