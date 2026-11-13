package org.jetbrains.plugins.scala.components

import consulo.project.Project
import consulo.project.ui.wm.WindowManager
import consulo.project.ui.wm.StatusBarWidgetsManager
import org.jetbrains.plugins.scala.extensions.executeOnPooledThread

object ScalaHighlightingModeWidgetUpdater {

  /**
   * @note Can be called on any thread. The updating logic will be run on a background, non-ui thread.
   */
  def scheduleWidgetUpdate(project: Project): Unit = executeOnPooledThread {
    updateWidgetImpl(project)
  }

  private def updateWidgetImpl(project: Project): Unit = {
    if (project.isDisposed)
      return
    StatusBarWidgetsManager.getInstance(project).updateWidget(classOf[ScalaHighlightingModeWidgetFactory], project.getUIAccess)
    val statusBar = WindowManager.getInstance().getStatusBar(project)
    if (statusBar eq null)
      return
    statusBar.updateWidget(ScalaHighlightingModeWidgetFactory.ID)
  }
}
