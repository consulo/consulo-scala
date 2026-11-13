package org.jetbrains.plugins.scala.lang.resolve

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.language.psi.ResolveResult
import org.jetbrains.plugins.scala.ExtensionPointDeclaration
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression

@ExtensionAPI(ComponentScope.APPLICATION)
abstract class DynamicTypeReferenceResolver {

  def resolve(expression: ScReferenceExpression): Array[ResolveResult]
}

object DynamicTypeReferenceResolver
  extends ExtensionPointDeclaration[DynamicTypeReferenceResolver](
    "org.intellij.scala.scalaDynamicTypeResolver"
  ) {

  def getAllResolveResult(expression: ScReferenceExpression): Seq[ResolveResult] =
    implementations.flatMap(_.resolve(expression))
}
