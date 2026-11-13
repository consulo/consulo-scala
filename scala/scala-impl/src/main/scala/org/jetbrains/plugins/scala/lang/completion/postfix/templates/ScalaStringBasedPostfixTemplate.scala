package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.codeEditor.Editor
import consulo.language.editor.postfixTemplate.PostfixTemplatesUtils
import consulo.language.editor.refactoring.postfixTemplate.{PostfixTemplateExpressionSelectorBase, PostfixTemplateWithExpressionSelector}
import consulo.language.editor.template.{Template, TemplateManager, TextExpression}
import consulo.language.psi.PsiElement

// upstream: extends com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate (absent in Consulo)
abstract class ScalaStringBasedPostfixTemplate(name: String,
                                               example: String,
                                               selector: PostfixTemplateExpressionSelectorBase)
  extends PostfixTemplateWithExpressionSelector(null, name, example, selector, null) {

  def getTemplateString(element: PsiElement): String

  def setVariables(template: Template, element: PsiElement): Unit = {}

  protected def shouldReformat: Boolean = true

  def getElementToRemove(expression: PsiElement): PsiElement = expression

  def createTemplate(manager: TemplateManager, templateString: String): Template = {
    val template = manager.createTemplate("", "", templateString)
    template.setToReformat(shouldReformat)
    template
  }

  override protected def expandForChooseExpression(expression: PsiElement, editor: Editor): Unit = {
    val project = expression.getProject
    val range = getElementToRemove(expression).getTextRange
    editor.getDocument.deleteString(range.getStartOffset, range.getEndOffset)
    val manager = TemplateManager.getInstance(project)

    getTemplateString(expression) match {
      case null =>
        PostfixTemplatesUtils.showErrorHint(project, editor)
      case templateString =>
        val template = createTemplate(manager, templateString)
        template.addVariable(ScalaStringBasedPostfixTemplate.EXPR, new TextExpression(expression.getText), false)
        setVariables(template, expression)
        manager.startTemplate(editor, template)
    }
  }
}

object ScalaStringBasedPostfixTemplate {
  final val EXPR = "expr"
}
