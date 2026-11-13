package org.jetbrains.plugins.scala.project.template

import consulo.language.codeStyle.{CodeStyle, CodeStyleSettingsManager}
import consulo.project.Project
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings

/**
 * Code style part of upstream `IndentationSyntaxStepLike` (IntelliJ new project wizard step, its UI is not ported).
 */
object IndentationSyntax {
  final val IndentationSyntaxPropertyName = "scala.IndentationSyntaxStepLike.useIndentationSyntax"

  def setupUseIndentationBasedSyntaxInProject(project: Project, useIndentationSyntax: Boolean): Unit = {
    val manager = CodeStyleSettingsManager.getInstance(project)
    var mainProjectCodeStyle = manager.getMainProjectCodeStyle
    if (mainProjectCodeStyle eq null) {
      val settings = CodeStyle.getSettings(project)
      mainProjectCodeStyle = manager.cloneSettings(settings)
    }
    val scalaCodeStyleSettings = mainProjectCodeStyle.getCustomSettings(classOf[ScalaCodeStyleSettings])
    scalaCodeStyleSettings.USE_SCALA3_INDENTATION_BASED_SYNTAX = useIndentationSyntax
    CodeStyle.setMainProjectSettings(project, mainProjectCodeStyle)
  }
}
