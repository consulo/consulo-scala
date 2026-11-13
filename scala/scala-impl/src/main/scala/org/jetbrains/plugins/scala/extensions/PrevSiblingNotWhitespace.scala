package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement

object PrevSiblingNotWhitespace {
  def unapply(e: PsiElement): Option[PsiElement] = Option(e.getPrevSiblingNotWhitespace)
}