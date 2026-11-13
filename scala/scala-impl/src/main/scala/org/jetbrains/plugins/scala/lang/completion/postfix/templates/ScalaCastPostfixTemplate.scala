package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.editor.template.TemplateManager
import consulo.language.editor.template.TextExpression
import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateWithExpressionSelector
import consulo.language.editor.postfixTemplate.PostfixTemplatesUtils
import consulo.document.Document
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector.SelectAllAncestors
import org.jetbrains.plugins.scala.lang.psi.api.expr._

final class ScalaCastPostfixTemplate extends PostfixTemplateWithExpressionSelector(
  null,
  "cast",
  "expr.asInstanceOf[SomeType]",
  SelectAllAncestors(),
  null
) with DumbAware {

  def getTemplateString(expression: PsiElement): String = "$expr$.asInstanceOf[$END$]"

  override def expandForChooseExpression(expression: PsiElement, editor: Editor): Unit = {
    val document: Document = editor.getDocument
    document.deleteString(expression.getTextRange.getStartOffset, expression.getTextRange.getEndOffset)
    val manager: TemplateManager = TemplateManager.getInstance(expression.getProject)

    val templateString = getTemplateString(expression)
    if (templateString == null) {
      PostfixTemplatesUtils.showErrorHint(expression.getProject, editor)
      return
    }


    val template = manager.createTemplate("", "", templateString)
    template.setToReformat(true)

    template.addVariable("expr", new TextExpression(expression match {
      case _: ScSugarCallExpr | _: ScDo | _: ScIf | _: ScTry | _: ScFor
           | _: ScWhile | _: ScThrow | _: ScReturn => "(" + expression.getText + ")"
      case _ => expression.getText
    }), false)

    manager.startTemplate(editor, template)
  }

}
