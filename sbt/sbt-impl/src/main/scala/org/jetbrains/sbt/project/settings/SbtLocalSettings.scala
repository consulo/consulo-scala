package org.jetbrains.sbt
package project.settings

import consulo.annotation.component.{ComponentScope, ServiceAPI, ServiceImpl}
import consulo.component.persist.{PersistentStateComponent, State, Storage, StoragePathMacros}
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings
import consulo.project.Project
import jakarta.inject.{Inject, Singleton}
import org.jetbrains.sbt.project.SbtProjectSystem

@State(
  name = "SbtLocalSettings",
  storages = Array[Storage](new Storage(StoragePathMacros.WORKSPACE_FILE))
)
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
final class SbtLocalSettings @Inject()(project: Project)
  extends AbstractExternalSystemLocalSettings(SbtProjectSystem.Id, project)
  with PersistentStateComponent[SbtLocalSettingsState] {

  override def getState: SbtLocalSettingsState = {
    val state = new SbtLocalSettingsState
    fillState(state)
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
