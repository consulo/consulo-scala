package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl
import consulo.project.Project
import org.jetbrains.bsp.settings.BspSettings
import org.jetbrains.sbt.project.autolink.SbtUnlinkedProjectAwareHelper

@ExtensionImpl
final class BspSbtUnlinkedProjectAwareHelper extends SbtUnlinkedProjectAwareHelper {

  override def isLinkedProject(project: Project, externalProjectPath: String): Boolean = {
    val settings = BspSettings.getInstance(project)
    settings.getLinkedProjectSettings(externalProjectPath) != null
  }
}
