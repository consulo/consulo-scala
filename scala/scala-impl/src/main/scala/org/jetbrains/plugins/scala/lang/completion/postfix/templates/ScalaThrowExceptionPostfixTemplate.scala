package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector._

final class ScalaThrowExceptionPostfixTemplate extends ScalaStringBasedPostfixTemplate(
  "throw",
  "throw expr",
  SelectTopmostAncestors(isSameOrInheritor("java.lang.Throwable"))
) {
  override def getTemplateString(element: PsiElement): String = "throw $expr$"
}
