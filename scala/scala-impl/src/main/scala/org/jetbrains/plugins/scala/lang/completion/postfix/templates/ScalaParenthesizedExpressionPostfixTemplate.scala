package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.editor.refactoring.postfixTemplate.ParenthesizedPostfixTemplate
import consulo.application.dumb.DumbAware
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector.SelectAllAncestors
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.ScalaPostfixTemplatePsiInfo

final class ScalaParenthesizedExpressionPostfixTemplate extends ParenthesizedPostfixTemplate(
  ScalaPostfixTemplatePsiInfo,
  SelectAllAncestors(),
  null
) with DumbAware
