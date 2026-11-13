package org.jetbrains.plugins.scala.settings.sections

import jakarta.inject.Inject

import consulo.annotation.component.ExtensionImpl

import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle

@ExtensionImpl
class BasePackagesSettingsSectionConfigurable @Inject()(project: Project) extends SettingsSectionConfigurable {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaBundle.message("scala.project.settings.form.tabs.base.packages"))
  override def createPanel(): SettingsSectionPanel = new BasePackagesSettingsSectionPanel(project)
}
