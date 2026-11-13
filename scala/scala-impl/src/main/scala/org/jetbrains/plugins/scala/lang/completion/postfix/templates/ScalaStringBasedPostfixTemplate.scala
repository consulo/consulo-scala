package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateExpressionSelectorBase
import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate
import consulo.language.psi.PsiElement

abstract class ScalaStringBasedPostfixTemplate(name: String,
                                               example: String,
                                               selector: PostfixTemplateExpressionSelectorBase)
  extends StringBasedPostfixTemplate(name, example, selector, null) {

  override def getElementToRemove(expression: PsiElement): PsiElement = expression
}
