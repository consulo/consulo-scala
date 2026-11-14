package org.jetbrains.plugins.scala.worksheet.actions.topmenu

import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.ex.action.{AnAction, AnActionEvent}
import org.jetbrains.plugins.scala.worksheet.WorksheetBundle
import org.jetbrains.plugins.scala.worksheet.actions.topmenu.StopWorksheetAction.StoppableProcess
import org.jetbrains.plugins.scala.worksheet.actions.{WorksheetActionUtil, WorksheetFileHook}

import consulo.ui.image.Image

class StopWorksheetAction(private var process: Option[StoppableProcess]) extends AnAction with TopComponentAction {

  override def genericText: String = WorksheetBundle.message("worksheet.stop.button")

  override def actionIcon: Image = PlatformIconGroup.actionsSuspend()

  override def update(e: AnActionEvent): Unit = {
    val isEnabledAndVisible = WorksheetActionUtil.getWorksheetFileFrom(e)
      .exists(file => WorksheetFileHook.isRunning(file.getVirtualFile))
    e.getPresentation.setEnabledAndVisible(isEnabledAndVisible)

    if (isEnabledAndVisible) {
      updateIconAndText(e)
    }
  }

  override def actionPerformed(e: AnActionEvent): Unit = process.foreach(_.stop())

  def setStoppableProcess(process: Option[StoppableProcess]): Unit = this.process = process
}

object StopWorksheetAction {

  trait StoppableProcess {
    def stop(): Unit
  }
}
