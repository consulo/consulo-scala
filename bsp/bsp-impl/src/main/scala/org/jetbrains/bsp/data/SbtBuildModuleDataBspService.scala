package org.jetbrains.bsp.data

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.DataNode
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.sbt.project.module.SbtModule.{Build, Imports}

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala

/** See also [[org.jetbrains.sbt.project.data.service.SbtBuildModuleDataService]] */
@ExtensionImpl
final class SbtBuildModuleDataBspService extends ScalaAbstractProjectDataService[SbtBuildModuleDataBsp, Module](SbtBuildModuleDataBsp.Key) {

  override def importData(toImport: util.Collection[DataNode[SbtBuildModuleDataBsp]],
                          project: Project,
                          synchronous: Boolean): Unit =
    BspModuleExtensions.executeProjectChangeAction(project, synchronous) {
      for {
        moduleNode <- toImport.asScala
        module <- project.getIdeModuleByNode(moduleNode)
      } {
        val buildModuleData = moduleNode.getData
        Imports(module) = buildModuleData.imports
        Build(module) = buildModuleData.id.uri
        // upstream: the Scala support of the build module comes from the sbt module type
        BspModuleExtensions.modifyModel(module)(BspModuleExtensions.enableScala)
      }
    }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()
}
