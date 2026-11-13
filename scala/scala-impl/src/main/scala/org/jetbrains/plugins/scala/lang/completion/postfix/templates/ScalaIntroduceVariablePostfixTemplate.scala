package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateWithExpressionSelector
import consulo.dataContext.DataContext
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector.SelectAllAncestors
import org.jetbrains.plugins.scala.lang.refactoring.introduceVariable.ScalaIntroduceVariableHandler

final class ScalaIntroduceVariablePostfixTemplate extends PostfixTemplateWithExpressionSelector(
  null,
  "var",
  "val name = expr",
  SelectAllAncestors(),
  null
) with DumbAware {

  override def expandForChooseExpression(expression: PsiElement, editor: Editor): Unit = {
    val range = expression.getTextRange
    val startOffset = range.getStartOffset
    val endOffset = range.getEndOffset

    editor.getSelectionModel.setSelection(startOffset, endOffset)
    new ScalaIntroduceVariableHandler()
      .invokeExpression(expression.getContainingFile, startOffset, endOffset)(using expression.getProject, editor, DataContext.EMPTY_CONTEXT)
  }
}
