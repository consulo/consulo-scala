package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.ExtensionPointDeclaration
import org.jetbrains.plugins.scala.lang.psi.types.ScType

/** Allows extensions to suppress inferred type hints without changing the definitions' types. */
@ExtensionAPI(ComponentScope.APPLICATION)
trait ScalaTypeHintFilter {
  /** Return true for definitions whose hints this filter does not suppress. */
  def shouldShowTypeHint(element: PsiElement, tpe: ScType, xRayMode: Boolean): Boolean
}

object ScalaTypeHintFilter extends ExtensionPointDeclaration[ScalaTypeHintFilter](
  "org.intellij.scala.typeHintFilter"
) {
  def shouldShowTypeHint(element: PsiElement, tpe: ScType, xRayMode: Boolean): Boolean =
    implementations.forall(_.shouldShowTypeHint(element, tpe, xRayMode))
}
