
package org.jetbrains.plugins.scala.lang.psi.stubs
package impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameterClause

class ScParamClauseStubImpl(parent: StubElement[? <: PsiElement],
                            elementType: IStubElementType[?, ?],
                            override val hasImplicitKeyword: Boolean,
                            override val hasUsingKeyword: Boolean,
                           )
  extends StubBase[ScParameterClause](parent, elementType) with ScParamClauseStub
