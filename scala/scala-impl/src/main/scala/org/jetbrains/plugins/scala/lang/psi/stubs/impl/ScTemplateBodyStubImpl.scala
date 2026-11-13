package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateBody
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTemplateBodyStub

class ScTemplateBodyStubImpl(parent: StubElement[? <: PsiElement],
                             elementType: IStubElementType[?, ?])
  extends StubBase[ScTemplateBody](parent, elementType) with ScTemplateBodyStub
