package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import consulo.application.util.SofterReference
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportSelector
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createReferenceFromText
import org.jetbrains.plugins.scala.lang.psi.stubs.ScImportSelectorStub

class ScImportSelectorStubImpl(parent: StubElement[? <: PsiElement],
                               elementType: IElementType,
                               override val referenceText: Option[String],
                               override val importedName: Option[String],
                               override val aliasName: Option[String],
                               override val isAliasedImport: Boolean,
                               override val isWildcardSelector: Boolean,
                               override val isGivenSelector: Boolean,
                               override val typeText: Option[String])
  extends StubBase[ScImportSelector](parent, elementType) with ScImportSelectorStub with PsiOwner[ScImportSelector] {

  private var referenceReference: SofterReference[Option[ScStableCodeReference]] = scala.compiletime.uninitialized

  override def reference: Option[ScStableCodeReference] = {
    getFromOptionalReference(referenceReference) {
      case (context, child) =>
        referenceText.map {
          createReferenceFromText(_, context, child)
        }
    } (referenceReference = _)
  }
}
