package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector._

final class ScalaPrintlnPostfixTemplate(alias: String = "sout") extends ScalaStringBasedPostfixTemplate(
  alias,
  "println(expr)",
  SelectTopmostAncestors(AnyExpression)
) with DumbAware {
  override def getTemplateString(element: PsiElement): String = "println($expr$)"
}
