package org.jetbrains.sbt.project

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.ui.AbstractExternalSystemToolWindowFactory
import consulo.ui.ex.toolWindow.ToolWindowAnchor
import consulo.ui.image.Image
import org.jetbrains.sbt.api.icons.Icons

// upstream: getSettings(project) override - the Consulo factory resolves the settings by the system id
@ExtensionImpl
class SbtToolWindowFactory extends AbstractExternalSystemToolWindowFactory(SbtProjectSystem.Id) {

  override def getAnchor: ToolWindowAnchor = ToolWindowAnchor.RIGHT

  override def getIcon: Image = Icons.SBT_TOOL_WINDOW
}
