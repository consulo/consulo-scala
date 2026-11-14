package org.jetbrains.plugins.scala.codeInsight.implicits

import consulo.ui.ex.action.{ActionUpdateThread, AnActionEvent}
import com.intellij.openapi.actionSystem.ToggleAction
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

class ShowImplicitHintsAction extends ToggleAction(
  ScalaCodeInsightBundle.message("show.implicit.hints.action.text"),
  ScalaCodeInsightBundle.message("show.implicit.hints.action.description"),
  /* icon = */ null
) {

  override def isSelected(event: AnActionEvent): Boolean = ImplicitHints.enabled

  override def setSelected(e: AnActionEvent, state: Boolean): Unit = {
    ImplicitHints.enabled = state
    ImplicitHints.updateInAllEditors()
    if (!state) {
      MouseHandler.removeEscKeyListeners()
    }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}

private object ShowImplicitHintsAction {
  val Id = "Scala.ShowImplicits"
}
