package org.jetbrains.sbt
package project

import consulo.annotation.component.ExtensionImpl


import org.jetbrains.sbt.api.asScala
import org.jetbrains.sbt.api.file
import org.jetbrains.sbt.api.find
import org.jetbrains.sbt.api.safeMap
import consulo.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemConfigLocator
import consulo.externalSystem.setting.ExternalProjectSettings
import consulo.virtualFileSystem.{LocalFileSystem, VirtualFile}

import java.nio.file.Path
import scala.jdk.CollectionConverters.*

@ExtensionImpl
class SbtConfigLocator extends ExternalSystemConfigLocator {
  override def getTargetExternalSystemId: ProjectSystemId = SbtProjectSystem.Id

  override def findAll(externalProjectSettings: ExternalProjectSettings): java.util.List[VirtualFile] = {
    val modules = externalProjectSettings.getModules.asScala
    modules.flatMap { path =>
      Option(LocalFileSystem.getInstance.refreshAndFindFileByNioFile(Path.of(path))).safeMap(adjust)
    }.toList.asJava
  }

  override def adjust(configPath: VirtualFile): VirtualFile = {
    // FIXME this is not a comprehensive sbt config location
    val buildSbt = configPath.find(Sbt.BuildFile)
    val buildScala = configPath.find(Sbt.ProjectDirectory).flatMap(_.find("Build.scala"))
    buildSbt.orElse(buildScala).orNull
  }
}
