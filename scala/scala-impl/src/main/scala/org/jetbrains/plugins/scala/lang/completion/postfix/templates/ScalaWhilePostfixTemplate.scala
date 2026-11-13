package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector.SelectTopmostAncestors

final class ScalaWhilePostfixTemplate extends ScalaStringBasedPostfixTemplate(
  "while",
  "while (expr) {}",
  SelectTopmostAncestors()
) {
  override def getTemplateString(element: PsiElement): String = "while ($expr$) {\n$END$\n}"
}
