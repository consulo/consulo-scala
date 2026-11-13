package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScExtensionBody
import org.jetbrains.plugins.scala.lang.psi.stubs.ScExtensionBodyStub

class ScExtensionBodyStubImpl(
  parent:      StubElement[? <: PsiElement],
  elementType: IStubElementType[?, ?]
) extends StubBase[ScExtensionBody](parent, elementType)
    with ScExtensionBodyStub
