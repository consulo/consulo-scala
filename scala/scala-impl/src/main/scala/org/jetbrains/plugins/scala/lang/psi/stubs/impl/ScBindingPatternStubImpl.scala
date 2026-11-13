package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.StubElement
import consulo.language.ast.IElementType
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.stubs.ScBindingPatternStub

class ScBindingPatternStubImpl[P <: ScBindingPattern](parent: StubElement[? <: PsiElement],
                                                      elementType: IElementType,
                                                      @Nullable name: String)
  extends ScNamedStubBase[P](parent, elementType, name) with ScBindingPatternStub[P]
