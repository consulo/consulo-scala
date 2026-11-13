package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.{PsiElement, SmartPsiElementPointer}

object ValidSmartPointer {

  def unapply[E <: PsiElement](pointer: SmartPsiElementPointer[E]): Option[E] =
    pointer.getElement match {
      case null => None
      case element if element.isValid => Some(element)
      case _ => None
    }
}
