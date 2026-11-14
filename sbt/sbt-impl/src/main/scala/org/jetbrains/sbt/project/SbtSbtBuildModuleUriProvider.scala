package org.jetbrains.sbt.project

import consulo.annotation.component.ExtensionImpl
import consulo.module.Module
import org.jetbrains.sbt.SbtUtil
import org.jetbrains.sbt.api.project.SbtBuildModuleUriProvider

import java.net.URI
import java.nio.file.Path

// upstream: reads SbtModuleEntity of the workspace model (WorkspaceModelUtil) - the imported SbtModuleData is used instead
@ExtensionImpl
final class SbtSbtBuildModuleUriProvider extends SbtBuildModuleUriProvider {
  override def getBuildModuleUri(module: Module): Option[URI] =
    SbtUtil.getSbtModuleData(module).map(_.buildURI.uri)

  override def getBuildModuleBaseDirectory(module: Module): Option[Path] =
    SbtUtil.getSbtModuleData(module).flatMap(data => Option(data._baseDirectoryPath).map(_.toPath).orElse(Option(data.baseDirectory).map(_.toPath)))
}
