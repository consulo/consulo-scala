package org.jetbrains.plugins.scala.settings

import consulo.configurable.Configurable
import consulo.ide.setting.ShowSettingsUtil
import consulo.project.{Project, ProjectManager}
import org.jetbrains.annotations.Nullable


/**
 * see [[consulo.ide.setting.ShowSettingsUtil]]
 */
object ShowSettingsUtilImplExt {

  // upstream: selects the configurable via ConfigurableVisitor and applies `filter` to the settings dialog
  def showSettingsDialog(@Nullable project: Project, configurableClass: Class[? <: Configurable], filter: String): Unit =
    ShowSettingsUtil.getInstance.showAndSelect(getProject(project), configurableClass)

  // upstream: selects the configurable of ScalaLanguageCodeStyleSettingsProvider
  def showScalaCodeStyleSettingsDialog(@Nullable project: Project, filter: String): Unit =
    ShowSettingsUtil.getInstance.showSettingsDialog(getProject(project), CodeStyleConfigurableId, filter)

  private val CodeStyleConfigurableId = "preferences.sourceCode"

  private def getProject(project: Project): Project =
    if (project != null) project
    else ProjectManager.getInstance.getDefaultProject
}
