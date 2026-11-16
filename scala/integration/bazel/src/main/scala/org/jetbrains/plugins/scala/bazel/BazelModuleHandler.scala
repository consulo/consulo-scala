package org.jetbrains.plugins.scala.bazel

import consulo.annotation.component.ExtensionImpl
import consulo.module.Module
import org.jetbrains.sbt.project.BuildToolModuleHandler

//noinspection ApiStatus
@ExtensionImpl
private final class BazelModuleHandler extends BuildToolModuleHandler {
  override def handles(module: Module): Boolean =
    org.jetbrains.bazel.config.project.isBazelProject(module.getProject)
}
