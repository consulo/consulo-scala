package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.annotation.component.ActionImpl

import consulo.application.AllIcons
import consulo.ui.ex.action.{ActionUpdateThread, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import consulo.codeEditor.Editor
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

@ActionImpl(id = "Scala.XRayMode")
class XRayModeAction extends ToggleAction(() => ScalaCodeInsightBundle.message("action.xray.mode"), AllIcons.Actions.Show) {
  override def isSelected(e: AnActionEvent): Boolean =
    ScalaHintsSettings.xRayMode && ScalaHintsSettings.xRayModePinned

  override def setSelected(e: AnActionEvent, state: Boolean): Unit = {
    CommonDataKeys.EDITOR.getData(e.getDataContext) match {
      case editor: Editor =>
        ScalaEditorFactoryListener.setXRayModeEnabled(state, editor)
      case null =>
        ScalaHintsSettings.xRayMode = state
    }
    ScalaHintsSettings.xRayModePinned = state
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}

object XRayModeAction {
  val ActionId = "Scala.XRayMode"
}
