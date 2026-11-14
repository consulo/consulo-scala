package org.jetbrains.plugins.scala.project.maven

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.module.Module
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenUtil
import org.jetbrains.sbt.project.BuildToolModuleHandler

//noinspection ApiStatus
@ExtensionImpl
private final class MavenModuleHandler extends BuildToolModuleHandler {
  override def handles(module: Module): Boolean =
    isMavenizedModule(module) || isExternalSystemModule(module)

  // upstream: MavenUtil.isMavenModule (workspace model based importer)
  private def isMavenizedModule(module: Module): Boolean =
    MavenProjectsManager.getInstance(module.getProject).isMavenizedModule(module)

  private def isExternalSystemModule(module: Module): Boolean =
    ExternalSystemApiUtil.isExternalSystemAwareModule(MavenUtil.SYSTEM_ID, module)
}
