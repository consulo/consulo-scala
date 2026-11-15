package org.jetbrains.plugins.scala.project.template

import consulo.language.codeStyle.{CodeStyle, CodeStyleSettingsManager}
import consulo.project.Project
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings

/**
 * Code style part of [[IndentationSyntaxStepLike]]: the project code style uses the indentation based syntax.
 */
object IndentationSyntax {
  final val IndentationSyntaxPropertyName = "scala.IndentationSyntaxStepLike.useIndentationSyntax"

  def setupUseIndentationBasedSyntaxInProject(project: Project, useIndentationSyntax: Boolean): Unit = {
    val manager = CodeStyleSettingsManager.getInstance(project)
    // upstream: manager.getMainProjectCodeStyle / manager.cloneSettings / CodeStyle.setMainProjectSettings
    var mainProjectCodeStyle = manager.PER_PROJECT_SETTINGS
    if (mainProjectCodeStyle eq null) {
      val settings = CodeStyle.getSettings(project)
      mainProjectCodeStyle = settings.clone()
    }
    val scalaCodeStyleSettings = mainProjectCodeStyle.getCustomSettings(classOf[ScalaCodeStyleSettings])
    scalaCodeStyleSettings.USE_SCALA3_INDENTATION_BASED_SYNTAX = useIndentationSyntax
    manager.PER_PROJECT_SETTINGS = mainProjectCodeStyle
    manager.USE_PER_PROJECT_SETTINGS = true
    manager.notifyCodeStyleSettingsChanged()
  }
}
