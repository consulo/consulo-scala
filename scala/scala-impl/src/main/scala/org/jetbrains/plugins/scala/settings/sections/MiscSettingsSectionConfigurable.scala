package org.jetbrains.plugins.scala.settings.sections

import consulo.annotation.component.ExtensionImpl

import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle

@ExtensionImpl
final class MiscSettingsSectionConfigurable(project: Project) extends SettingsSectionConfigurable {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaBundle.message("scala.project.settings.form.tabs.misc"))
  override def createPanel(): SettingsSectionPanel = new MiscSettingsSectionPanel(project)
}
