package org.jetbrains.plugins.scala.lang.psi.stubs

import consulo.language.psi.PsiElement

trait ScTopLevelElementStub[T <: PsiElement] extends ScStubElement[T] {
  def isTopLevel: Boolean
  def topLevelQualifier: Option[String]
}
