package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPatternList
import org.jetbrains.plugins.scala.lang.psi.stubs.ScPatternListStub

class ScPatternListStubImpl(parent: StubElement[? <: PsiElement],
                            elementType: IStubElementType[?, ?],
                            override val simplePatterns: Boolean)
  extends StubBase[ScPatternList](parent, elementType) with ScPatternListStub
