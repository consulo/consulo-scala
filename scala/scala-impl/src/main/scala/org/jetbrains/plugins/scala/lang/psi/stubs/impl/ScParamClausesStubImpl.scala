package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameters
import org.jetbrains.plugins.scala.lang.psi.stubs.ScParamClausesStub

class ScParamClausesStubImpl(parent: StubElement[? <: PsiElement],
                             elementType: IStubElementType[?, ?])
  extends StubBase[ScParameters](parent, elementType) with ScParamClausesStub
