package org.jetbrains.plugins.scala.server.compiler
import consulo.project.Project
import org.jetbrains.plugins.scala.compilersettings.settings.ScalaCompileServerSettings
import org.jetbrains.plugins.scala.settings.ShowSettingsUtilImplExt

object CompileServerSettingsUtil {
  def showCompileServerSettingsDialog(project: Project, filter: String = ""): Unit =
    ShowSettingsUtilImplExt.showSettingsDialog(project, classOf[ScalaCompileServerForm], filter)

  def enableCompileServer(): Unit = {
    val settings = ScalaCompileServerSettings.getInstance()
    settings.COMPILE_SERVER_ENABLED = true
  }
}
