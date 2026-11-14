package org.jetbrains.plugins.scala.worksheet.actions.topmenu

import consulo.ui.ex.action.{AnAction, AnActionEvent, AnActionWithSyncUpdate}
import consulo.ui.ex.keymap.KeymapManager
import consulo.ui.ex.keymap.util.KeymapUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.worksheet.actions.WorksheetAction

import consulo.ui.image.Image

trait TopComponentAction extends AnActionWithSyncUpdate with WorksheetAction {
  this: AnAction =>

  @Nls
  def genericText: String

  def actionIcon: Image

  def shortcutId: Option[String] = None

  override def update(e: AnActionEvent): Unit = {
    updatePresentationEnabled(e)
    updateIconAndText(e)
  }

  protected def updateIconAndText(e: AnActionEvent): Unit = {
    e.getPresentation.setIcon(actionIcon)
    e.getPresentation.setText(displayText)
  }

  // These actions rely on PSI file and module search that must be executed on BGT (AnActionWithSyncUpdate)

  private def displayText: String = {
    val shortcutHintText = for {
      id <- shortcutId
      shortcuts = KeymapManager.getInstance.getActiveKeymap.getShortcuts(id)
      shortcut <- shortcuts.headOption
    } yield s" (${KeymapUtil.getShortcutText(shortcut)})"

    genericText + shortcutHintText.mkString
  }
}
