package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.{PsiElement, PsiReference}

object ReferenceTarget {
  def unapply(e: PsiReference): Option[PsiElement] = Option(e.resolve)
}