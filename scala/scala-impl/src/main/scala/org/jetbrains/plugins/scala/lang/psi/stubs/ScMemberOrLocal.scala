package org.jetbrains.plugins.scala.lang.psi.stubs


import org.jetbrains.plugins.scala.api.lang.psi.stubs.ScStubElement
import consulo.language.psi.PsiElement

trait ScMemberOrLocal[T <: PsiElement] extends ScStubElement[T] {
  def isLocal: Boolean
}
