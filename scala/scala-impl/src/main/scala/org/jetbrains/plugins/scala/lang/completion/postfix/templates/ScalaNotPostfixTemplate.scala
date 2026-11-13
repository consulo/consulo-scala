package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.editor.refactoring.postfixTemplate.NotPostfixTemplate
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector._
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.ScalaPostfixTemplatePsiInfo

import scala.annotation.nowarn

@nowarn("cat=deprecation")
final class ScalaNotPostfixTemplate(alias: String = "not", needsDotInKey: Boolean = true) extends NotPostfixTemplate(
  alias,
  if (needsDotInKey) "." + alias else alias,
  "!expr",
  ScalaPostfixTemplatePsiInfo,
  SelectAllAncestors(BooleanExpression)
)
