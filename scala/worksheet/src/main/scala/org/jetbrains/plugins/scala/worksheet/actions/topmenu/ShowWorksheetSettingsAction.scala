package org.jetbrains.plugins.scala.worksheet.actions.topmenu

import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.ex.action.{AnAction, AnActionEvent}
import org.jetbrains.plugins.scala.worksheet.WorksheetBundle
import org.jetbrains.plugins.scala.worksheet.settings.ui.WorksheetSettingsDialog

import consulo.ui.image.Image

class ShowWorksheetSettingsAction extends AnAction with TopComponentAction {

  override def genericText: String = WorksheetBundle.message("worksheet.settings.button")

  override def actionIcon: Image = PlatformIconGroup.generalSettings()

  override def actionPerformed(e: AnActionEvent): Unit = {
    for {(_, psiFile) <- getCurrentScalaWorksheetEditorAndFile(e)} {
      val dialog = new WorksheetSettingsDialog(psiFile)
      dialog.show()
    }
  }
}
