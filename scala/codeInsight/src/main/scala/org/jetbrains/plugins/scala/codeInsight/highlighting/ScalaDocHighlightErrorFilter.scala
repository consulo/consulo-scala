package org.jetbrains.plugins.scala
package codeInsight
package highlighting

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.HighlightErrorFilter
import consulo.language.psi.PsiErrorElement
import org.jetbrains.plugins.scala.extensions.{IteratorExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocComment

@ExtensionImpl
final class ScalaDocHighlightErrorFilter extends HighlightErrorFilter {

  override def shouldHighlightErrorElement(element: PsiErrorElement): Boolean = !isInScaladoc(element)

  private def isInScaladoc(element: PsiErrorElement) =
    element.parentsInFile.filterByType[ScDocComment].nonEmpty
}
