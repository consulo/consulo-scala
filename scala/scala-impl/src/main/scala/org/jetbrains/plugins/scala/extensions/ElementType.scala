package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiElement
import consulo.language.ast.IElementType

object ElementType {
  def unapply(e: PsiElement): Some[IElementType] = Some(e.getNode.getElementType)
}