package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement

object ElementText {
  def unapply(e: PsiElement): Some[String] = Some(e.getText)
}