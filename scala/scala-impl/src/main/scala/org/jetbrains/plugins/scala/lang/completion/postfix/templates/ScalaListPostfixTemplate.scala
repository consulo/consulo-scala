package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector.SelectAllAncestors

final class ScalaListPostfixTemplate extends ScalaStringBasedPostfixTemplate(
  "List",
  "List(expr)",
  SelectAllAncestors()
) with DumbAware {
  override def getTemplateString(element: PsiElement): String = "List($expr$)"
}
