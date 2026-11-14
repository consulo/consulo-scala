package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl

import consulo.module.Module
import org.jetbrains.sbt.api.project.SbtVersionProvider

@ExtensionImpl
final class BspSbtVersionProvider extends SbtVersionProvider {
  override def getSbtVersion(module: Module): Option[String] = {
    val sbtBuildModuleData = BspExternalSystemUtil.getSbtBuildModuleDataBsp(module)
    sbtBuildModuleData.map(_.sbtVersion)
  }
}
