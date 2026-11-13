package org.jetbrains.plugins.scala.lang.psi.stubs.impl


import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import org.jetbrains.plugins.scala.lang.psi.api.base.ScIdList
import org.jetbrains.plugins.scala.lang.psi.stubs.ScIdListStub

class ScIdListStubImpl[ParentPsi <: PsiElement](parent: StubElement[ParentPsi],
                                                  elemType: IElementType)
  extends StubBase[ScIdList](parent, elemType) with ScIdListStub
