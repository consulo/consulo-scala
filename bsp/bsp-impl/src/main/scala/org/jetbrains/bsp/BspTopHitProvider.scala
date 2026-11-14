package org.jetbrains.bsp

import consulo.annotation.component.ExtensionImpl

import consulo.configurable.search.ActionsTopHitProvider

@ExtensionImpl
class BspTopHitProvider extends ActionsTopHitProvider {
  override protected def getActionsMatrix: Array[Array[String]] = Array(
      Array("b", "bsp ", "ExternalSystem.RefreshAllProjects"),
      Array("reload all b", "reload all bsp Projects ", "ExternalSystem.RefreshAllProjects"),
      Array("load b", "load bsp changes ", "ExternalSystem.ProjectRefreshAction")
    )
}
