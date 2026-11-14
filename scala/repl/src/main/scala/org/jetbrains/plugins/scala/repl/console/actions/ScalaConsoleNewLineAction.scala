package org.jetbrains.plugins.scala.repl.console.actions

import consulo.annotation.component.ActionImpl
import consulo.dataContext.DataContext
import consulo.ui.ex.action.IdeActions
import com.intellij.openapi.editor.actionSystem.{EditorAction, EditorWriteActionHandler}
import consulo.codeEditor.action.{EditorActionHandler, EditorActionManager}
import consulo.codeEditor.{Caret, Editor}
import org.jetbrains.plugins.scala.repl.console.{ScalaConsoleInfo, ScalaReplBundle}

@ActionImpl(id = "ScalaConsole.NewLine")
class ScalaConsoleNewLineAction extends EditorAction(new EditorWriteActionHandler(true) {

  override def isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean =
    ScalaConsoleInfo.isConsole(editor) &&
      getEnterHandler.isEnabled(editor, caret, dataContext)

  override def executeWriteAction(editor: Editor, caret: Caret, dataContext: DataContext): Unit =
    getEnterHandler.execute(editor, caret, dataContext)

  private def getEnterHandler: EditorActionHandler =
    EditorActionManager.getInstance.getActionHandler(IdeActions.ACTION_EDITOR_ENTER)
}) {
  locally {
    val presentation = getTemplatePresentation
    presentation.setText(ScalaReplBundle.message("scalaconsole.new.line.in.repl"))
    presentation.setDescription(ScalaReplBundle.message("scalaconsole.new.line.in.repl"))
  }
}