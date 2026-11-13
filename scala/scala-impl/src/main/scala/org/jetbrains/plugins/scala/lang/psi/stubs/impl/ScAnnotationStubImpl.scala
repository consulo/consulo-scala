package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import consulo.language.psi.PsiElement
import consulo.language.psi.stub.{StubBase, StubElement}
import consulo.language.ast.IElementType
import consulo.application.util.SofterReference
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScAnnotation, ScAnnotationExpr}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.stubs.ScAnnotationStub

class ScAnnotationStubImpl(parent: StubElement[? <: PsiElement],
                           elementType: IElementType,
                           override val annotationText: String,
                           override val name: Option[String])
  extends StubBase[ScAnnotation](parent, elementType) with ScAnnotationStub with PsiOwner[ScAnnotation] {

  private[impl] var annotationExprRef: SofterReference[Option[ScAnnotationExpr]] = scala.compiletime.uninitialized

  override def annotationExpr: Option[ScAnnotationExpr] = {
    getFromOptionalReference(annotationExprRef) {
      case (context, _) =>
        val annotation = ScalaPsiElementFactory.createAnAnnotation(annotationText, context)(using getProject)
        val annotationExpr = annotation.annotationExpr
        annotationExpr.context = context
        Some(annotationExpr)
    } (annotationExprRef = _)

  }

  override def typeElement: Option[ScTypeElement] = annotationExpr.map(_.constructorInvocation.typeElement)
}
