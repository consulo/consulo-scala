package org.jetbrains.plugins.scala.settings.sections

import jakarta.inject.Inject

import consulo.annotation.component.ExtensionImpl

import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.help.ScalaWebHelpProvider

@ExtensionImpl
class PerformanceSettingsSectionConfigurable @Inject()(project: Project) extends SettingsSectionConfigurable {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaBundle.message("scala.project.settings.form.tabs.performance"))
  override def createPanel(): SettingsSectionPanel = new PerformanceSettingsSectionPanel(project)
  override def getHelpTopic: String = ScalaWebHelpProvider.HelpPrefix + "scala-improvingperformance.html"
}
