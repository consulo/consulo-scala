package org.jetbrains.plugins.scala.settings.sections

import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle

class BasePackagesSettingsSectionConfigurable(project: Project) extends SettingsSectionConfigurable {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaBundle.message("scala.project.settings.form.tabs.base.packages"))
  override def createPanel(): SettingsSectionPanel = new BasePackagesSettingsSectionPanel(project)
}
