package org.jetbrains.plugins.scala.lang.psi.light

import consulo.language.psi.PsiNamedElement
import consulo.language.impl.psi.LightElement
import org.jetbrains.plugins.scala.extensions.ObjectExt

object isWrapper {
  def unapply(lightElem: LightElement): Option[PsiNamedElement] = lightElem match {
    case PsiClassWrapper(definition) => Some(definition)
    case methodWrapper: PsiMethodWrapper[_] => methodWrapper.delegate.asOptionOfUnsafe[PsiNamedElement]
    case _ => None
  }
}
