package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScEnumCases
import org.jetbrains.plugins.scala.lang.psi.stubs.ScEnumCasesStub

class ScEnumCasesStubImpl(parent: StubElement[? <: PsiElement],
                          elementType: IElementType)
  extends StubBase[ScEnumCases](parent, elementType) with ScEnumCasesStub
