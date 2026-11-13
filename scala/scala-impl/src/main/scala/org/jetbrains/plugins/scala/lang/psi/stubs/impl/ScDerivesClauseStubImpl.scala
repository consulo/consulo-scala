package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScDerivesClause
import org.jetbrains.plugins.scala.lang.psi.stubs.ScDerivesClauseStub

class ScDerivesClauseStubImpl(
  parent:      StubElement[? <: PsiElement],
  elementType: IStubElementType[?, ?]
) extends StubBase[ScDerivesClause](parent, elementType)
    with ScDerivesClauseStub
