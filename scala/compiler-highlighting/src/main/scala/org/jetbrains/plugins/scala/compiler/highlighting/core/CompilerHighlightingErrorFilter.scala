package org.jetbrains.plugins.scala.compiler.highlighting.core

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.HighlightErrorFilter
import consulo.language.psi.PsiErrorElement
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode

@ExtensionImpl
final class CompilerHighlightingErrorFilter extends HighlightErrorFilter {
  override def shouldHighlightErrorElement(element: PsiErrorElement): Boolean =
    !ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(element.getContainingFile)
}
