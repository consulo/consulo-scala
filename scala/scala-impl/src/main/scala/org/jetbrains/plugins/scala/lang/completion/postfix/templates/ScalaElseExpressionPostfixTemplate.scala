package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.editor.refactoring.postfixTemplate.ElseExpressionPostfixTemplateBase
import consulo.application.dumb.DumbAware
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector.SelectTopmostAncestors
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.ScalaPostfixTemplatePsiInfo
import org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression.ScalaWithIfConditionSurrounder

final class ScalaElseExpressionPostfixTemplate extends ElseExpressionPostfixTemplateBase(
  ScalaPostfixTemplatePsiInfo,
  SelectTopmostAncestors(ScalaWithIfConditionSurrounder)
) with DumbAware {
  override def getSurrounder: ScalaWithIfConditionSurrounder.type = ScalaWithIfConditionSurrounder
}
