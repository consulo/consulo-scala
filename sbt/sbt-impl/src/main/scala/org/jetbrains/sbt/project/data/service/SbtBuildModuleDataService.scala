package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl

import consulo.externalSystem.model.DataNode
import consulo.externalSystem.service.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.sbt.project.data.SbtBuildModuleData
import org.jetbrains.sbt.project.module.SbtModule

import java.util
import scala.jdk.CollectionConverters._

@ExtensionImpl
final class SbtBuildModuleDataService extends ScalaAbstractProjectDataService[SbtBuildModuleData, Module](SbtBuildModuleData.Key) {

  override def importData(
    toImport: util.Collection[? <: DataNode[SbtBuildModuleData]],
    projectData: ProjectData,
    project: Project,
    modelsProvider: IdeModifiableModelsProvider
  ): Unit =
    for {
      moduleNode <- toImport.asScala
      module     <- modelsProvider.getIdeModuleByNode(moduleNode)
    } importData(module, moduleNode.getData)

  private def importData(sbtModule: Module,
                         data: SbtBuildModuleData): Unit = {
    val SbtBuildModuleData(imports, resolvers, buildFor) = data

    SbtModule.Imports(sbtModule) = imports
    SbtModule.Resolvers(sbtModule) = resolvers.asScala.toSet
    SbtModule.Build(sbtModule) = buildFor.uri
  }
}
