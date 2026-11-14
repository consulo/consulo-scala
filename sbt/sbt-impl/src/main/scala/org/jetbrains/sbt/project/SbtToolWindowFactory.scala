package org.jetbrains.sbt.project

import consulo.annotation.component.ExtensionImpl

import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory
import consulo.externalSystem.setting.AbstractExternalSystemSettings
import consulo.project.Project
import org.jetbrains.sbt.settings.SbtSettings

@ExtensionImpl(id = "sbt")
class SbtToolWindowFactory extends AbstractExternalSystemToolWindowFactory(SbtProjectSystem.Id) {

  override def getSettings(project: Project): AbstractExternalSystemSettings[?, ?, ?] =
    SbtSettings.getInstance(project)
}