package org.jetbrains.plugins.scala.lang.psi.stubs.factories

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubElement, StubInputStream}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScEnumCases
import org.jetbrains.plugins.scala.lang.psi.impl.statements.ScEnumCasesImpl
import org.jetbrains.plugins.scala.lang.psi.stubs.ScEnumCasesStub
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScEnumCasesElementType
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.ScEnumCasesStubImpl

final class ScEnumCasesStubFactory(elementType: ScEnumCasesElementType) extends ScStubSerializingElementFactory[ScEnumCasesStub, ScEnumCases](elementType) {
  override def createPsi(stub: ScEnumCasesStub): ScEnumCases =
    new ScEnumCasesImpl(stub, new ScEnumCasesElementType, null)

  override def createStubImpl(psi: ScEnumCases, parentStub: StubElement[? <: PsiElement]): ScEnumCasesStub =
    new ScEnumCasesStubImpl(parentStub, elementType)

  override def deserialize(dataStream: StubInputStream, parentStub: StubElement[? <: PsiElement]): ScEnumCasesStub =
    new ScEnumCasesStubImpl(parentStub, elementType)
}
