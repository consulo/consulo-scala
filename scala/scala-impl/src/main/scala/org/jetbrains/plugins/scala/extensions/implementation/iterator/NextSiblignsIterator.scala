package org.jetbrains.plugins.scala.extensions.implementation.iterator

import consulo.language.psi.PsiElement

class NextSiblignsIterator(element: PsiElement) extends Iterator[PsiElement] {
  private var current = Option(element).map(_.getNextSibling).orNull

  override def hasNext: Boolean = current != null

  override def next(): PsiElement = {
    val result = current
    current = current.getNextSibling
    result
  }
}