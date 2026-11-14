package org.jetbrains.plugins.scala.lang.refactoring.ui

import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.ui.annotation.RequiredUIAccess
import consulo.ui.{Hyperlink, ToolTip}
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.util.TypeAnnotationUtil

object TypeAnnotationsSettingsLink {

  @RequiredUIAccess
  def create(project: Project): Hyperlink = {
    val link = Hyperlink.create(LocalizeValue.of(ScalaBundle.message("default.ta.settings")))
    link.addHyperlinkListener(_ => TypeAnnotationUtil.showTypeAnnotationsSettings(project))
    link.setToolTip(ToolTip.create().setDescription(LocalizeValue.of(ScalaBundle.message("default.ta.tooltip"))))
    link
  }
}
