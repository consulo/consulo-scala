package org.jetbrains.plugins.scala.settings.sections

import consulo.annotation.component.ExtensionImpl

import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.help.ScalaWebHelpProvider

@ExtensionImpl
class WorksheetSettingsSectionConfigurable(project: Project) extends SettingsSectionConfigurable {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaBundle.message("scala.project.settings.form.tabs.worksheet"))
  override def createPanel(): SettingsSectionPanel = new WorksheetSettingsSectionPanel(project)
  override def getHelpTopic: String = ScalaWebHelpProvider.HelpPrefix + "work-with-scala-worksheet-and-ammonite.html" // TODO: update after DOC-37979 is fixed
}
