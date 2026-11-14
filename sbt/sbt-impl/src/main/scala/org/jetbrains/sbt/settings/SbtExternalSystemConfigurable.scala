package org.jetbrains.sbt.settings

import consulo.annotation.component.ExtensionImpl
import consulo.configurable.{ProjectConfigurable, StandardConfigurableIds}
import consulo.externalSystem.service.setting.AbstractExternalSystemConfigurable
import consulo.project.Project
import jakarta.inject.Inject
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.settings.{SbtProjectSettings, SbtProjectSettingsListener}

// upstream: the project/system settings controls are created by SbtSettingsConfigurableFactory;
// the TraverseUIMode linking of a default project setting for the options index is dropped
@ExtensionImpl
class SbtExternalSystemConfigurable @Inject()(project: Project)
  extends AbstractExternalSystemConfigurable[SbtProjectSettings, SbtProjectSettingsListener, SbtSettings](project, SbtProjectSystem.Id)
    with ProjectConfigurable {

  override def getId: String = "sbt.project.settings.configurable"

  override def getParentId: String = StandardConfigurableIds.EXECUTION_GROUP
}
