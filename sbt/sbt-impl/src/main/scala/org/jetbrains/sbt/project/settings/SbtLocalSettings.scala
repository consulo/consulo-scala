package org.jetbrains.sbt
package project.settings

import consulo.component.persist.{PersistentStateComponent, Storage, StoragePathMacros}
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings
import consulo.project.Project
import org.jetbrains.sbt.project.SbtProjectSystem

@State(
  name = "SbtLocalSettings",
  storages = Array(new Storage(StoragePathMacros.WORKSPACE_FILE))
)
@Service(Array(Service.Level.PROJECT))
final class SbtLocalSettings(project: Project)
  extends AbstractExternalSystemLocalSettings[SbtLocalSettingsState](SbtProjectSystem.Id, project)
  with PersistentStateComponent[SbtLocalSettingsState] {

  override def getState: SbtLocalSettingsState = {
    val state = new SbtLocalSettingsState
    state
  }

  override def loadState(state: SbtLocalSettingsState): Unit = {
    super[AbstractExternalSystemLocalSettings].loadState(state)
  }
}

class SbtLocalSettingsState extends AbstractExternalSystemLocalSettings.State {
}

object SbtLocalSettings {
  def getInstance(project: Project): SbtLocalSettings = project.getInstance(classOf[SbtLocalSettings])
}
