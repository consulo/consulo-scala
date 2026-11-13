package org.jetbrains.plugins.scala.lang.psi.stubs.elements

import consulo.language.ast.ASTNode
import consulo.language.Language
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{IStubElementType, IndexSink, StubElement, StubInputStream, StubOutputStream}
import consulo.language.impl.ast.FileElement
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType.ScExpressionElementType
import org.jetbrains.plugins.scala.lang.parser.{ScCodeBlockElementType, SelfPsiCreator}
import org.jetbrains.plugins.scala.lang.psi.stubs.ScalaStubRegistryExtension
import org.jetbrains.plugins.scala.lang.psi.stubs.factories.ScStubSerializingElementFactory
import org.jetbrains.plugins.scala.util.UnloadableThreadLocal

import scala.annotation.tailrec

// upstream: element types are plain IElementTypes with stub support registered separately via
// StubSerializingElementFactory; Consulo has only IStubElementType, so stub operations are delegated
// to the factory registered for this element type in ScalaStubRegistryExtension.
abstract class ScStubElementType[T <: PsiElement](debugName: String, language: Language = ScalaLanguage.INSTANCE)
  extends IStubElementType[StubElement[T], T](debugName, language)
    with SelfPsiCreator {
  override def createElement(node: ASTNode): T

  private def factory: ScStubSerializingElementFactory[StubElement[T], T] =
    ScalaStubRegistryExtension.factoryFor(this).asInstanceOf[ScStubSerializingElementFactory[StubElement[T], T]]

  override final def getExternalId: String = s"${getLanguage.getID.toLowerCase}.${super.toString}"

  override def createPsi(stub: StubElement[T]): T = factory.createPsi(stub)

  override def createStub(psi: T, parentStub: StubElement[?]): StubElement[T] =
    factory.createStub(psi, parentStub.asInstanceOf[StubElement[? <: PsiElement]])

  override def serialize(stub: StubElement[T], dataStream: StubOutputStream): Unit = factory.serialize(stub, dataStream)

  override def deserialize(dataStream: StubInputStream, parentStub: StubElement[?]): StubElement[T] =
    factory.deserialize(dataStream, parentStub.asInstanceOf[StubElement[? <: PsiElement]])

  override def indexStub(stub: StubElement[T], sink: IndexSink): Unit = factory.indexStub(stub, sink)

  override def shouldCreateStub(node: ASTNode): Boolean = factory.shouldCreateStub(node)

  override final def isLeftBound = true

  override final def toString: String = super.toString
}

object ScStubElementType {

  @tailrec
  private[stubs] def isLocal(node: ASTNode): Boolean = node match {
    case _: FileElement | null => false
    case _ =>
      node.getElementType match {
        case _: ScTemplateDefinitionElementType[_] => false
        case _: ScExpressionElementType | _: ScCodeBlockElementType => true
        case _ => isLocal(node.getTreeParent)
      }
  }

  object Processing {
    private val flag = new UnloadableThreadLocal[Long](0)

    def run[R](action: => R): R =
      try {
        flag.update(_ + 1)
        action
      } finally {
        flag.update(_ - 1)
      }

    def isRunning: Boolean = flag.value > 0
  }
}
