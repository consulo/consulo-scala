package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl
import consulo.configurable.{ProjectConfigurable, StandardConfigurableIds}
import consulo.externalSystem.service.setting.AbstractExternalSystemConfigurable
import consulo.project.Project
import jakarta.inject.Inject
import org.jetbrains.bsp._
import org.jetbrains.bsp.settings._

// upstream: the project/system settings controls are created by BspSettingsConfigurableFactory;
// the TraverseUIMode linking of a default project setting for the options index and the web help topic are dropped
@ExtensionImpl
class BspExternalSystemConfigurable @Inject()(project: Project)
  extends AbstractExternalSystemConfigurable[BspProjectSettings, BspProjectSettingsListener, BspSettings](project, BSP.ProjectSystemId)
    with ProjectConfigurable {

  override def getId: String = "bsp.project.settings.configurable"

  override def getParentId: String = StandardConfigurableIds.EXECUTION_GROUP
}
