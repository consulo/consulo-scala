package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAccessModifier
import org.jetbrains.plugins.scala.lang.psi.stubs.ScAccessModifierStub

class ScAccessModifierStubImpl(parent: StubElement[? <: PsiElement],
                               elementType: IStubElementType[?, ?],
                               override val isProtected: Boolean,
                               override val isPrivate: Boolean,
                               override val isThis: Boolean,
                               override val idText: Option[String])
  extends StubBase[ScAccessModifier](parent, elementType) with ScAccessModifierStub
