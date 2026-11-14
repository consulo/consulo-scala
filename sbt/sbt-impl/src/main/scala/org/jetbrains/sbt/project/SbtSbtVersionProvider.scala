package org.jetbrains.sbt.project

import consulo.annotation.component.ExtensionImpl


import org.jetbrains.sbt.api.project.SbtVersionProvider
import consulo.module.Module
import org.jetbrains.sbt.settings.SbtSettings

@ExtensionImpl
final class SbtSbtVersionProvider extends SbtVersionProvider {
  override def getSbtVersion(module: Module): Option[String] = {
    val sbtProjectSettings = SbtSettings.getInstance(module.getProject).getLinkedProjectSettings(module)
    sbtProjectSettings.flatMap(s => Option(s.sbtVersion))
  }
}
