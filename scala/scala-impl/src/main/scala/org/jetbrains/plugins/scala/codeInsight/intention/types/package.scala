package org.jetbrains.plugins.scala.codeInsight.intention

import consulo.language.editor.template.TemplateManager
import consulo.language.editor.template.TemplateBuilder
import consulo.language.editor.template.TemplateBuilderFactory
import consulo.language.editor.template.{Expression, TemplateManager}
import consulo.language.editor.template.event.TemplateEditingListener
import consulo.codeEditor.Editor
import consulo.language.psi.{PsiDocumentManager, PsiElement}

package object types {

  def startTemplate(
    elem: PsiElement,
    context: PsiElement,
    expression: Expression,
    editor: Editor
  ): Unit = {
    startTemplate(elem, context, expression, editor, null)
  }

  def startTemplate(
    elem: PsiElement,
    context: PsiElement,
    expression: Expression,
    editor: Editor,
    listener: TemplateEditingListener,
  ): Unit = {
    val project = context.getProject
    val manager = PsiDocumentManager.getInstance(project)
    manager.commitAllDocuments()
    manager.doPostponedOperationsAndUnblockDocument(editor.getDocument)

    val builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(elem)
    builder.replaceElement(elem, expression)

    editor.getCaretModel.moveToOffset(elem.getNode.getStartOffset)

    val templateManager = TemplateManager.getInstance(project)
    templateManager.startTemplate(editor, builder.buildInlineTemplate(), listener)
  }
}
