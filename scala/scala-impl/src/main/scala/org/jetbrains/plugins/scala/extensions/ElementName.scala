package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiNamedElement

object ElementName {
  def unapply(e: PsiNamedElement): Some[String] = Some(e.name)
}