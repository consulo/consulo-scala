package org.jetbrains.plugins.scala.lang.psi.impl.toplevel

import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScMember
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaStubBasedElementImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTopLevelElementStub

trait ScTopLevelStubBasedElement[T <: PsiElement, S <: ScTopLevelElementStub[T]]
    extends ScalaStubBasedElementImpl[T, S]
    with ScMember {

  override def isTopLevel: Boolean               = byStubOrPsi(_.isTopLevel)(super.isTopLevel)
  override def topLevelQualifier: Option[String] = byStubOrPsi(_.topLevelQualifier)(super.topLevelQualifier)
}
