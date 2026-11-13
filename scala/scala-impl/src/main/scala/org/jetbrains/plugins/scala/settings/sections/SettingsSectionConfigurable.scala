package org.jetbrains.plugins.scala.settings.sections

import consulo.configurable.{Configurable, ConfigurationException}
import consulo.disposer.Disposable
import consulo.ui.Component
import consulo.ui.ex.awtUnsafe.TargetAWT

abstract class SettingsSectionConfigurable extends Configurable {

  def createPanel(): SettingsSectionPanel

  private var currentPanel = Option.empty[SettingsSectionPanel]

  override def createUIComponent(parentDisposable: Disposable): Component = {
    val panel = currentPanel.getOrElse {
      val panel = createPanel()
      currentPanel = Some(panel)
      panel
    }
    TargetAWT.wrap(panel.getRootPanel)
  }

  override def isModified: Boolean = currentPanel.get.isModified
  @throws[ConfigurationException]
  override def apply(): Unit = currentPanel.get.apply()
  override def reset(): Unit = currentPanel.get.reset()
  override def disposeUIResources(): Unit = {
    currentPanel = None
  }
}
