package org.jetbrains.plugins.scala.lang.psi.stubs


import org.jetbrains.plugins.scala.api.lang.psi.stubs.ScStubElement
import consulo.language.psi.PsiElement

trait ScTopLevelElementStub[T <: PsiElement] extends ScStubElement[T] {
  def isTopLevel: Boolean
  def topLevelQualifier: Option[String]
}
