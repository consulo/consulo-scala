package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParamClause
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTypeParamClauseStub

class ScTypeParamClauseStubImpl(parent: StubElement[? <: PsiElement],
                                elementType: IStubElementType[?, ?],
                                override val typeParameterClauseText: String)
  extends StubBase[ScTypeParamClause](parent, elementType) with ScTypeParamClauseStub
