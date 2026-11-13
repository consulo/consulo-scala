package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor
import org.jetbrains.plugins.scala.lang.psi.stubs.ScPrimaryConstructorStub

class ScPrimaryConstructorStubImpl(parent: StubElement[? <: PsiElement],
                                   elementType: IStubElementType[?, ?])
  extends StubBase[ScPrimaryConstructor](parent, elementType) with ScPrimaryConstructorStub
