package org.jetbrains.plugins.scala.repl.console.actions

import consulo.annotation.component.ActionImpl
import consulo.dataContext.DataContext
import consulo.localize.LocalizeValue
import consulo.codeEditor.{Caret, Editor}
import consulo.codeEditor.action.{EditorAction, EditorWriteActionHandler}
import org.jetbrains.plugins.scala.repl.console.{ScalaConsoleInfo, ScalaReplBundle}

@ActionImpl(id = "ScalaConsole.Clean")
class ScalaConsoleCleanAction extends EditorAction(
  LocalizeValue.of(ScalaReplBundle.message("clean.scala.repl.content.menu.action.text")),
  LocalizeValue.of(ScalaReplBundle.message("clean.scala.repl.content.menu.action.description")),
  new EditorWriteActionHandler(false) {

  override def isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean =
    ScalaConsoleInfo.isConsole(editor)

  override def executeWriteAction(editor: Editor, caret: Caret, dataContext: DataContext): Unit =
    ScalaConsoleInfo.getConsole(editor) match {
      case null =>
      case console => console.clear()
    }
})
