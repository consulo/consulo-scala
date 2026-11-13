package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAccessModifier
import org.jetbrains.plugins.scala.lang.psi.stubs.ScAccessModifierStub

class ScAccessModifierStubImpl(parent: StubElement[? <: PsiElement],
                               elementType: IElementType,
                               override val isProtected: Boolean,
                               override val isPrivate: Boolean,
                               override val isThis: Boolean,
                               override val idText: Option[String])
  extends StubBase[ScAccessModifier](parent, elementType) with ScAccessModifierStub
