package org.jetbrains.plugins.scala.project.gradle

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.gradle.GradleConstants
import consulo.module.Module
import org.jetbrains.sbt.project.BuildToolModuleHandler

//noinspection ApiStatus
@ExtensionImpl
private final class GradleModuleHandler extends BuildToolModuleHandler {
  override def handles(module: Module): Boolean =
    ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)
}
