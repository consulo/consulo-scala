package org.jetbrains.sbt.project

import consulo.annotation.component.ExtensionImpl

import consulo.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.ui.ExternalSystemTextProvider
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle

@ExtensionImpl
private final class SbtTextProvider extends ExternalSystemTextProvider {
  override def getSystemId: ProjectSystemId = SbtProjectSystem.Id

  override def getUPNLinkActionText: String =
    ExternalSystemBundle.message("unlinked.project.notification.load.action", getSystemId.getReadableName)
}
