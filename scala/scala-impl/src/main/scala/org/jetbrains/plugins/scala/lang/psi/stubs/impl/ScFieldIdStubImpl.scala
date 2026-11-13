package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.StubElement
import org.jetbrains.plugins.scala.lang.psi.api.base.ScFieldId
import org.jetbrains.plugins.scala.lang.psi.stubs.ScFieldIdStub

class ScFieldIdStubImpl(parent: StubElement[? <: PsiElement],
                        elementType: IStubElementType[?, ?],
                        name: String)
  extends ScNamedStubBase[ScFieldId](parent, elementType, name) with ScFieldIdStub
