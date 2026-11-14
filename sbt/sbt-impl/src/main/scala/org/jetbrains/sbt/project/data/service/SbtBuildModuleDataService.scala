package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.DataNode
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
    toImport: util.Collection[DataNode[SbtBuildModuleData]],
    project: Project,
    synchronous: Boolean
  ): Unit = SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
    val projectJdks = new SbtModuleExtensions.ProjectJdkCache(project)
    for {
      moduleNode <- toImport.asScala
      module     <- project.getIdeModuleByNode(moduleNode)
    } {
      importData(module, moduleNode.getData)
      // upstream: the build module inherits the project JDK (ModuleSdkData) and its Scala support comes from the sbt module type.
      // Consulo: the build module gets the Java extension with the JDK of the sbt project and the Scala extension
      SbtModuleExtensions.modifyModel(module) { model =>
        SbtModuleExtensions.configureJava(model, projectJdks.jdkFor(moduleNode), languageLevel = None)
        SbtModuleExtensions.enableScala(model)
      }
    }
  }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()

  private def importData(sbtModule: Module,
                         data: SbtBuildModuleData): Unit = {
    val SbtBuildModuleData(imports, resolvers, buildFor) = data

    SbtModule.Imports(sbtModule) = imports
    SbtModule.Resolvers(sbtModule) = resolvers.asScala.toSet
    SbtModule.Build(sbtModule) = buildFor.uri
  }
}
