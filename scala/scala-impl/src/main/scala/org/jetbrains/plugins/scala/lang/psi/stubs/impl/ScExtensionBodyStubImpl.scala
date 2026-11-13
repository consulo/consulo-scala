package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScExtensionBody
import org.jetbrains.plugins.scala.lang.psi.stubs.ScExtensionBodyStub

class ScExtensionBodyStubImpl(
  parent:      StubElement[? <: PsiElement],
  elementType: IElementType
) extends StubBase[ScExtensionBody](parent, elementType)
    with ScExtensionBodyStub
