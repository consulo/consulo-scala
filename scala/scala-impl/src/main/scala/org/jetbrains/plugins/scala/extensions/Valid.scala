package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement

object Valid {
  def unapply(element: PsiElement): Option[PsiElement] = Option(element).filter(_.isValid)
}
