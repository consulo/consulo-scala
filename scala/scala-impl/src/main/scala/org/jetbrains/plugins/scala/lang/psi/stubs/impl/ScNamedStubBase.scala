package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.{NamedStub, StubBase, StubElement}
import consulo.language.ast.IElementType
import consulo.language.psi.{PsiElement, PsiNamedElement}
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.api.lang.psi.stubs.ScStubElement

abstract class ScNamedStubBase[E <: PsiNamedElement] protected[impl](parent: StubElement[? <: PsiElement],
                                                                     elementType: IElementType,
                                                                     @Nullable name: String)
  extends StubBase[E](parent, elementType) with NamedStub[E] with ScStubElement[E] {

  @Nullable
  override final def getName: String = name
}
