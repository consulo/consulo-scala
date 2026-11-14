package org.jetbrains.plugins.scala.server.compiler

import consulo.annotation.component.ExtensionImpl
import consulo.project.Project
import consulo.project.ui.wm.{StatusBar, StatusBarWidget, StatusBarWidgetFactory}
import consulo.ui.image.{Image, ImageEffects}
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.compilersettings.settings.ScalaCompileServerSettings

@ExtensionImpl(id = "ScalaCompileServer", order = "before ScalaHighlightingMode")
private final class CompileServerWidgetFactory extends StatusBarWidgetFactory {
  override def getId: String = CompileServerWidgetFactory.ID

  override def getDisplayName: String = ServerManagementBundle.message("scala.compile.server.title")

  override def isAvailable(project: Project): Boolean =
    CompileServerLauncher.running || canBeEnabled(project)

  override def createWidget(project: Project): StatusBarWidget = new CompileServerWidget(project)

  override def canBeEnabledOn(statusBar: StatusBar): Boolean = {
    val project = statusBar.getProject
    if (project == null) return false
    canBeEnabled(project)
  }

  override def isEnabledByDefault: Boolean = false

  private def canBeEnabled(project: Project): Boolean =
    ScalaCompileServerSettings.getInstance().COMPILE_SERVER_ENABLED && project.hasScala
}

private object CompileServerWidgetFactory {
  val ID: String = "ScalaCompileServer"

  val IconRunning: Image = Icons.COMPILE_SERVER
  val IconStopped: Image = ImageEffects.grayed(IconRunning)
}
