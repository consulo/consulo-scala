package org.jetbrains.bsp

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.ProjectSystemId
import consulo.externalSystem.ui.ExternalSystemIconProvider
import consulo.ui.image.Image

@ExtensionImpl
class BspIconProvider extends ExternalSystemIconProvider {
  override def getSystemId: ProjectSystemId = BSP.ProjectSystemId

  override def getReloadIcon: Image = Icons.BSP_LOAD_CHANGES
}
