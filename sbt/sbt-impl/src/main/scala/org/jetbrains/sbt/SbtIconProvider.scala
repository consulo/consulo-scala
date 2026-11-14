package org.jetbrains.sbt

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.ProjectSystemId
import consulo.externalSystem.ui.ExternalSystemIconProvider
import consulo.ui.image.Image
import org.jetbrains.sbt.api.icons.Icons
import org.jetbrains.sbt.project.SbtProjectSystem

@ExtensionImpl
class SbtIconProvider extends ExternalSystemIconProvider {
  override def getSystemId: ProjectSystemId = SbtProjectSystem.Id

  override def getReloadIcon: Image = Icons.SBT_LOAD_CHANGES
}
