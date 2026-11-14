package org.jetbrains.bsp

import consulo.annotation.component.ExtensionImpl
import consulo.logging.Logger
import consulo.project.Project
import consulo.project.ui.wm.{StatusBar, StatusBarWidget, StatusBarWidgetFactory}
import consulo.ui.image.{Image, ImageEffects}

@ExtensionImpl(id = "BSP", order = "before ScalaCompileServer")
private final class BspServerWidgetFactory extends StatusBarWidgetFactory {
  override def getId: String = BspServerWidgetFactory.ID

  override def getDisplayName: String = BspBundle.message("bsp.widget.bsp.connection")

  override def isAvailable(project: Project): Boolean = BspUtil.isBspProject(project)

  override def createWidget(project: Project): StatusBarWidget = new BspServerWidget()

  override def canBeEnabledOn(statusBar: StatusBar): Boolean = {
    val project = statusBar.getProject
    if (project == null) return false
    isAvailable(project)
  }
}

private object BspServerWidgetFactory {
  val ID: String = "BSP"

  val IconRunning: Image = Icons.BSP
  val IconStopped: Image = ImageEffects.grayed(IconRunning)
  val logger: Logger = Logger.getInstance(classOf[BspServerWidgetFactory])

  type UpdateWidgetListener = BspServerWidgetUpdateListener

  val Topic: Class[BspServerWidgetUpdateListener] = classOf[BspServerWidgetUpdateListener]
}
