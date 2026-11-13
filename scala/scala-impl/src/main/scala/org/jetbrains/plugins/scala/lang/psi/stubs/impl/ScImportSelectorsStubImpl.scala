package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportSelectors
import org.jetbrains.plugins.scala.lang.psi.stubs.ScImportSelectorsStub

class ScImportSelectorsStubImpl(parent: StubElement[? <: PsiElement],
                                elementType: IStubElementType[?, ?],
                                override val hasWildcard: Boolean)
  extends StubBase[ScImportSelectors](parent, elementType) with ScImportSelectorsStub
