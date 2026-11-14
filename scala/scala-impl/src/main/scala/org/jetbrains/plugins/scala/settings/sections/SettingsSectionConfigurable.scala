package org.jetbrains.plugins.scala.settings.sections

import consulo.configurable.{ConfigurationException, ProjectConfigurable}
import consulo.disposer.Disposable
import consulo.ui.Component
import consulo.ui.annotation.RequiredUIAccess
import org.jetbrains.plugins.scala.settings.ScalaProjectSettingsConfigurable

abstract class SettingsSectionConfigurable extends ProjectConfigurable {

  // upstream: projectConfigurable parentId="configurable.scala.root" without explicit id (class name was used)
  override def getId: String = getClass.getName

  override def getParentId: String = ScalaProjectSettingsConfigurable.ID

  @RequiredUIAccess
  def createPanel(): SettingsSectionPanel

  private var currentPanel = Option.empty[SettingsSectionPanel]

  @RequiredUIAccess
  override def createUIComponent(parentDisposable: Disposable): Component = {
    val panel = currentPanel.getOrElse {
      val panel = createPanel()
      currentPanel = Some(panel)
      panel
    }
    panel.getRootPanel
  }

  @RequiredUIAccess
  override def isModified: Boolean = currentPanel.get.isModified
  @RequiredUIAccess
  @throws[ConfigurationException]
  override def apply(): Unit = currentPanel.get.apply()
  @RequiredUIAccess
  override def reset(): Unit = currentPanel.get.reset()
  @RequiredUIAccess
  override def disposeUIResources(): Unit = {
    currentPanel = None
  }
}
