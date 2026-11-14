package org.jetbrains.plugins.scala.repl.console.actions

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent, AnActionWithSyncUpdate}
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.document.util.TextRange
import org.jetbrains.plugins.scala.actions.ScalaActionUtil
import org.jetbrains.plugins.scala.repl.console.{ScalaConsoleInfo, ScalaLanguageConsole, ScalaReplBundle}
import org.jetbrains.plugins.scala.extensions
import org.jetbrains.plugins.scala.extensions.OptionExt
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

import java.io.{IOException, OutputStream}

@ActionImpl(id = "Scala.SendSelectionToConsole", parents = Array(new ActionParentRef(value = new ActionRef(id = "RunContextPopupGroup"))))
class SendSelectionToConsoleAction extends AnAction(
  ScalaReplBundle.message("send.selection.to.scala.repl.menu.action.text"),
  ScalaReplBundle.message("send.selection.to.scala.repl.menu.action.description"),
  Icons.SCALA_CONSOLE
) with AnActionWithSyncUpdate {

  override def update(e: AnActionEvent): Unit = {
    val isVisibleAndEnabled = (for {
      scalaFile <- ScalaActionUtil.getFileFrom(e).filterByType[ScalaFile]

      editor <- Option(e.getData(Editor.KEY))
      if editor.getSelectionModel.hasSelection

      console <- Option(ScalaConsoleInfo.getConsole(scalaFile.getProject))
      if !console.getConsoleEditor.isDisposed

      processHandler <- Option(ScalaConsoleInfo.getProcessHandler(scalaFile.getProject))
      if !processHandler.isProcessTerminated
    } yield true).isDefined
    
    e.getPresentation.setEnabledAndVisible(isVisibleAndEnabled)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def actionPerformed(e: AnActionEvent): Unit = {
    val context = e.getDataContext
    val editor = context.getData(Editor.KEY)
    val project = context.getData(Project.KEY)

    if (editor == null || project == null) return
    val selectedText = editor.getSelectionModel.getSelectedText
    val console = ScalaConsoleInfo.getConsole(project)
    if (console != null) sendSelection(console, selectedText)
  }

  private def sendSelection(console: ScalaLanguageConsole, text: String): Unit = {
    val consoleEditor = console.getConsoleEditor
    val controller = ScalaConsoleInfo.getController(console.getProject)
    val processHandler = ScalaConsoleInfo.getProcessHandler(console.getProject)

    if (consoleEditor != null) {
      val document = console.getEditorDocument
      console.setInputText(text)

      extensions.inWriteAction {
        val range: TextRange = new TextRange(0, document.getTextLength)
        consoleEditor.getSelectionModel.setSelection(range.getStartOffset, range.getEndOffset)
        console.addToHistory(range, console.getConsoleEditor, true)
        controller.addToHistory(text)

        consoleEditor.getCaretModel.moveToOffset(0)
        consoleEditor.getDocument.setText("")
      }

      text.split('\n').foreach(line => {
        if (line != "") {
          val outputStream: OutputStream = processHandler.getProcessInput
          try {
            val bytes: Array[Byte] = (line + "\n").getBytes
            outputStream.write(bytes)
            outputStream.flush()
          }
          catch {
            case _: IOException => //ignore
          }
        }
        console.textSent(line + "\n")
      })
    }
  }
}
