package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAnnotations
import org.jetbrains.plugins.scala.lang.psi.stubs.ScAnnotationsStub

class ScAnnotationsStubImpl(parent: StubElement[? <: PsiElement],
                            elementType: IElementType)
  extends StubBase[ScAnnotations](parent, elementType) with ScAnnotationsStub
