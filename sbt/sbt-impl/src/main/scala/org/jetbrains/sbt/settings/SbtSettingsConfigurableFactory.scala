package org.jetbrains.sbt.settings

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.ProjectSystemId
import consulo.externalSystem.service.setting.{AbstractExternalProjectSettingsConfigurable, ExternalSystemSettingsConfigurable, ExternalSystemSettingsConfigurableFactory, ExternalSystemSettingsPlace}
import consulo.project.ProjectManager
import consulo.ui.annotation.RequiredUIAccess
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.settings.Context.Configuration
import org.jetbrains.sbt.project.settings.{SbtProjectSettings, SbtProjectSettingsControl}

@ExtensionImpl
class SbtSettingsConfigurableFactory extends ExternalSystemSettingsConfigurableFactory[SbtProjectSettings, SbtSettings] {

  override def getSystemId: ProjectSystemId = SbtProjectSystem.Id

  override def createProjectSettings(): SbtProjectSettings = new SbtProjectSettings()

  override def createSystemSettings(): SbtSettings = new SbtSettings(ProjectManager.getInstance().getDefaultProject)

  @RequiredUIAccess
  override def createProjectSettingsConfigurable(
    settings: SbtProjectSettings,
    place: ExternalSystemSettingsPlace
  ): AbstractExternalProjectSettingsConfigurable[SbtProjectSettings] =
    new SbtProjectSettingsControl(Configuration, settings)

  @RequiredUIAccess
  override def createSystemSettingsConfigurable(
    settings: SbtSettings,
    place: ExternalSystemSettingsPlace
  ): ExternalSystemSettingsConfigurable[SbtSettings] =
    new SbtSettingsControl(settings)
}
