package org.jetbrains.plugins.scala.lang.completion.postfix.templates

import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateWithExpressionSelector
import consulo.language.editor.postfixTemplate.PostfixTemplatesUtils
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElement
import consulo.language.codeStyle.CodeStyleManager
import org.jetbrains.plugins.scala.lang.completion.postfix.templates.selector.AncestorSelector.SelectAllAncestors
import org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression.ScalaWithMatchSurrounder

/**
 * @see [[ScalaExhaustiveMatchPostfixTemplate]]
 */
final class ScalaMatchPostfixTemplate extends PostfixTemplateWithExpressionSelector(
  null,
  ScalaWithMatchSurrounder.getTemplateDescription,
  "expr match {...}",
  SelectAllAncestors(ScalaWithMatchSurrounder),
  null
) with DumbAware {

  override def expandForChooseExpression(expression: PsiElement, editor: Editor): Unit = {
    val file = expression.getContainingFile // not to be inlined!
    val project = expression.getProject

    PostfixTemplatesUtils.surround(ScalaWithMatchSurrounder, editor, expression) match {
      case null =>
      case range =>
        val styleManager = CodeStyleManager.getInstance(project)
        editor.getCaretModel.moveToOffset(range.getStartOffset)
        styleManager.adjustLineIndent(file, range)
    }
  }
}
