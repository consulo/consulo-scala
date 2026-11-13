package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScEarlyDefinitions
import org.jetbrains.plugins.scala.lang.psi.stubs.ScEarlyDefinitionsStub

class ScEarlyDefinitionsStubImpl(parent: StubElement[? <: PsiElement],
                                 elementType: IStubElementType[?, ?])
  extends StubBase[ScEarlyDefinitions](parent, elementType) with ScEarlyDefinitionsStub
