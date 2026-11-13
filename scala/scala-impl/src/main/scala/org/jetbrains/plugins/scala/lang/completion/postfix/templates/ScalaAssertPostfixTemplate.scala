package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector.SelectTopmostAncestors

final class ScalaAssertPostfixTemplate extends ScalaStringBasedPostfixTemplate(
  "assert",
  "assert(expr)",
  SelectTopmostAncestors()
) {
  override def getTemplateString(element: PsiElement): String = "assert($expr$)"
}
