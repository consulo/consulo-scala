package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement

object PrevSiblingNotWhitespaceComment {
  def unapply(e: PsiElement): Option[PsiElement] = Option(e.getPrevSiblingNotWhitespaceComment)
}
