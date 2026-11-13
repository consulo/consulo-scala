package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import org.jetbrains.plugins.scala.api.lang.psi.stubs.ScStubElement
import consulo.language.psi.PsiElement
import consulo.application.util.SofterReference
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionWithContextFromText

trait ScExpressionOwnerStub[E <: PsiElement] extends ScStubElement[E] with PsiOwner[E] {

  def bodyText: Option[String]

  private[impl] var expressionElementReference: SofterReference[Option[ScExpression]] = scala.compiletime.uninitialized

  def bodyExpression: Option[ScExpression] = {
    getFromOptionalReference(expressionElementReference) {
      case (context, child) =>
        bodyText.map {
          createExpressionWithContextFromText(_, context, child)
        }
    } (expressionElementReference = _)
  }
}