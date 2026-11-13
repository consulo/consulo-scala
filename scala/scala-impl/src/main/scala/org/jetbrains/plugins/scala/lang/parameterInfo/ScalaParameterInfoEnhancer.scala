package org.jetbrains.plugins.scala.lang.parameterInfo

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import org.jetbrains.plugins.scala.ExtensionPointDeclaration
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.types.PhysicalMethodSignature

@ExtensionAPI(ComponentScope.APPLICATION)
abstract class ScalaParameterInfoEnhancer {
  def enhance(signature: PhysicalMethodSignature, arguments: Seq[ScExpression]): Seq[PhysicalMethodSignature]
}

object ScalaParameterInfoEnhancer
  extends ExtensionPointDeclaration[ScalaParameterInfoEnhancer](
    "org.intellij.scala.parameterInfoEnhancer"
  ) {

  def enhance(signature: PhysicalMethodSignature, arguments: Seq[ScExpression]): Seq[PhysicalMethodSignature] =
    implementations.flatMap(_.enhance(signature, arguments))
}
