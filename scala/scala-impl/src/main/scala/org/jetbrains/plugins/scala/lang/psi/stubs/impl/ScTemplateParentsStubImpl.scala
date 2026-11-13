package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.application.util.SofterReference
import org.jetbrains.plugins.scala.lang.psi.api.base.ScConstructorInvocation
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScTemplateParents
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createConstructorFromText
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTemplateParentsStub

import scala.collection.immutable.ArraySeq


final class ScTemplateParentsStubImpl(
  parent:                         StubElement[? <: PsiElement],
  elementType:                    IStubElementType[?, ?],
  override val parentClausesText: Array[String]
) extends StubBase[ScTemplateParents](parent, elementType)
    with ScTemplateParentsStub
    with PsiOwner[ScTemplateParents] {

  private var constructorAndParentTypeElementsReference: SofterReference[Seq[ScConstructorInvocation]] = scala.compiletime.uninitialized

  override def parentClauses: Seq[ScConstructorInvocation] =
    getFromReference(constructorAndParentTypeElementsReference) { case (context, child) =>
        val parentElems = parentClausesText.map(
          createConstructorFromText(_, context, child)
        )

        ArraySeq.unsafeWrapArray(parentElems)
      } (constructorAndParentTypeElementsReference = _)
}
