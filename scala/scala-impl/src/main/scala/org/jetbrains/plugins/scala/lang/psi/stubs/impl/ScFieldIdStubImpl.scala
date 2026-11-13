package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.StubElement
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.base.ScFieldId
import org.jetbrains.plugins.scala.lang.psi.stubs.ScFieldIdStub

class ScFieldIdStubImpl(parent: StubElement[? <: PsiElement],
                        elementType: IElementType,
                        name: String)
  extends ScNamedStubBase[ScFieldId](parent, elementType, name) with ScFieldIdStub
