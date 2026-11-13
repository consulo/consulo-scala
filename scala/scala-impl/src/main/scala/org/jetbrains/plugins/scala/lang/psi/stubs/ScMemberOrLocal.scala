package org.jetbrains.plugins.scala.lang.psi.stubs

import consulo.language.psi.PsiElement

trait ScMemberOrLocal[T <: PsiElement] extends ScStubElement[T] {
  def isLocal: Boolean
}
