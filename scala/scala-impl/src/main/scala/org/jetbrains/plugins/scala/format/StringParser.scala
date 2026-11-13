package org.jetbrains.plugins.scala.format

import consulo.language.psi.PsiElement

trait StringParser extends StringParserLike[PsiElement]

trait StringParserLike[E <: PsiElement] {
  def parse(element: E): Option[Seq[StringPart]]
}