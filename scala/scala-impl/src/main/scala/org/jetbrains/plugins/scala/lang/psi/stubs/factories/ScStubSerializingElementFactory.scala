package org.jetbrains.plugins.scala.lang.psi.stubs.factories

import consulo.language.ast.{ASTNode, IElementType}
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{IndexSink, StubElement, StubInputStream, StubOutputStream}
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScStubElementType

// upstream: implements the platform StubSerializingElementFactory (absent in Consulo);
// here it is the delegate of ScStubElementType (an IStubElementType)
abstract class ScStubSerializingElementFactory[Stub <: StubElement[?], Psi <: PsiElement](
  elementType: IElementType,
) {

  def serialize(stub: Stub, dataStream: StubOutputStream): Unit = {}

  def deserialize(dataStream: StubInputStream, parentStub: StubElement[? <: PsiElement]): Stub

  def indexStub(stub: Stub, sink: IndexSink): Unit = {}

  def shouldCreateStub(node: ASTNode): Boolean = !ScStubElementType.isLocal(node)

  final def createStub(psi: Psi, parentStub: StubElement[? <: PsiElement]): Stub = ScStubElementType.Processing.run {
    createStubImpl(psi, parentStub)
  }

  protected def createStubImpl(psi: Psi, parentStub: StubElement[? <: PsiElement]): Stub

  def createPsi(stub: Stub): Psi
}
