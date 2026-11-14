package org.jetbrains.bsp

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.ui.AbstractExternalSystemToolWindowFactory
import consulo.ui.ex.toolWindow.ToolWindowAnchor
import consulo.ui.image.Image

// upstream: getSettings(project) override - the Consulo factory resolves the settings by the system id
@ExtensionImpl
class BspToolWindowFactory extends AbstractExternalSystemToolWindowFactory(BSP.ProjectSystemId) {

  override def getAnchor: ToolWindowAnchor = ToolWindowAnchor.RIGHT

  override def getIcon: Image = Icons.BSP_TOOLWINDOW
}
