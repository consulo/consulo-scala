package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.annotation.component.ActionImpl

import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.ex.action.{AnActionEvent}
import consulo.ui.ex.action.ToggleAction
import consulo.codeEditor.Editor
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle

@ActionImpl(id = "Scala.XRayMode")
class XRayModeAction extends ToggleAction(LocalizeValue.of(ScalaCodeInsightBundle.message("action.xray.mode")), LocalizeValue.empty(), PlatformIconGroup.actionsShow()) {
  override def isSelected(e: AnActionEvent): Boolean =
    ScalaHintsSettings.xRayMode && ScalaHintsSettings.xRayModePinned

  override def setSelected(e: AnActionEvent, state: Boolean): Unit = {
    e.getData(Editor.KEY) match {
      case editor: Editor =>
        ScalaEditorFactoryListener.setXRayModeEnabled(state, editor)
      case null =>
        ScalaHintsSettings.xRayMode = state
    }
    ScalaHintsSettings.xRayModePinned = state
  }
}

object XRayModeAction {
  val ActionId = "Scala.XRayMode"
}
