package org.jetbrains.plugins.scala.actions

import consulo.ui.ex.action.{ActionUpdateThread, AnAction, AnActionEvent}
import consulo.language.editor.CommonDataKeys
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.actions.ScalaHighlightingModeAction.perform
import org.jetbrains.plugins.scala.settings.ShowSettingsUtilImplExt
import org.jetbrains.plugins.scala.settings.sections.EditorSettingsSectionConfigurable

final class ScalaHighlightingModeAction extends AnAction(
  ScalaBundle.message("scala.highlighting.mode.action.text"),
  ScalaBundle.message("scala.highlighting.mode.action.description"),
  /* icon = */ null
) {
  override def actionPerformed(e: AnActionEvent): Unit = {
    e.getDataContext.getData(CommonDataKeys.PROJECT) match {
      case project: Project => perform(project)
      case null =>
    }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object ScalaHighlightingModeAction {
  def perform(project: Project): Unit =
    ShowSettingsUtilImplExt.showSettingsDialog(project, classOf[EditorSettingsSectionConfigurable], filter = "")
}
