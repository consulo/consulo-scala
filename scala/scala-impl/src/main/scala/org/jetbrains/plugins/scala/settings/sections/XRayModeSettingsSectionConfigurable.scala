package org.jetbrains.plugins.scala.settings.sections

import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.help.ScalaWebHelpProvider

class XRayModeSettingsSectionConfigurable(project: Project) extends SettingsSectionConfigurable {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaBundle.message("scala.project.settings.form.tabs.xray.mode"))
  override def createPanel(): SettingsSectionPanel = new XRayModeSettingsSectionPanel(project)
  override def getHelpTopic: String = ScalaWebHelpProvider.HelpPrefix + "scala-x-ray-mode.html"
}
