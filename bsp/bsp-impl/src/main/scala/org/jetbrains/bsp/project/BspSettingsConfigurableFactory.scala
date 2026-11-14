package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.ProjectSystemId
import consulo.externalSystem.service.setting.{AbstractExternalProjectSettingsConfigurable, ExternalSystemSettingsConfigurable, ExternalSystemSettingsConfigurableFactory, ExternalSystemSettingsPlace}
import consulo.project.ProjectManager
import consulo.ui.annotation.RequiredUIAccess
import org.jetbrains.bsp.BSP
import org.jetbrains.bsp.settings.{BspProjectSettings, BspProjectSettingsControl, BspSettings, BspSystemSettingsControl}

// upstream: AbstractExternalSystemConfigurable.createProjectSettingsControl/createSystemSettingsControl/newProjectSettings
@ExtensionImpl
class BspSettingsConfigurableFactory extends ExternalSystemSettingsConfigurableFactory[BspProjectSettings, BspSettings] {

  override def getSystemId: ProjectSystemId = BSP.ProjectSystemId

  override def createProjectSettings(): BspProjectSettings = new BspProjectSettings

  override def createSystemSettings(): BspSettings = new BspSettings(ProjectManager.getInstance().getDefaultProject)

  @RequiredUIAccess
  override def createProjectSettingsConfigurable(
    settings: BspProjectSettings,
    place: ExternalSystemSettingsPlace
  ): AbstractExternalProjectSettingsConfigurable[BspProjectSettings] =
    new BspProjectSettingsControl(settings, place)

  @RequiredUIAccess
  override def createSystemSettingsConfigurable(
    settings: BspSettings,
    place: ExternalSystemSettingsPlace
  ): ExternalSystemSettingsConfigurable[BspSettings] =
    new BspSystemSettingsControl(settings)
}
