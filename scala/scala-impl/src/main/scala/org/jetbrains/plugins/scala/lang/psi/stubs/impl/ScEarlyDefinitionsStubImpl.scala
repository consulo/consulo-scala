package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScEarlyDefinitions
import org.jetbrains.plugins.scala.lang.psi.stubs.ScEarlyDefinitionsStub

class ScEarlyDefinitionsStubImpl(parent: StubElement[? <: PsiElement],
                                 elementType: IElementType)
  extends StubBase[ScEarlyDefinitions](parent, elementType) with ScEarlyDefinitionsStub
