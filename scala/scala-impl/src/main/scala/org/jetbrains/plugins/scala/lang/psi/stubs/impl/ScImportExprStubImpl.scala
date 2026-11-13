package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.stub.IStubElementType
import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.application.util.SofterReference
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportExpr
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createReferenceFromText
import org.jetbrains.plugins.scala.lang.psi.stubs.ScImportExprStub

class ScImportExprStubImpl(parent: StubElement[? <: PsiElement],
                           elementType: IStubElementType[?, ?],
                           override val referenceText: Option[String],
                           override val hasWildcardSelector: Boolean,
                           override val hasGivenSelector: Boolean)
  extends StubBase[ScImportExpr](parent, elementType) with ScImportExprStub with PsiOwner[ScImportExpr] {

  private var referenceReference: SofterReference[Option[ScStableCodeReference]] = null

  override def reference: Option[ScStableCodeReference] = {
    getFromOptionalReference(referenceReference) {
      case (context, child) =>
        referenceText.map {
          createReferenceFromText(_, context, child)
        }
    } (referenceReference = _)
  }
}
