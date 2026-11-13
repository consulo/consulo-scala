package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScAnnotations
import org.jetbrains.plugins.scala.lang.psi.stubs.ScAnnotationsStub

class ScAnnotationsStubImpl(parent: StubElement[? <: PsiElement],
                            elementType: IStubElementType[?, ?])
  extends StubBase[ScAnnotations](parent, elementType) with ScAnnotationsStub
