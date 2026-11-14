package org.jetbrains.sbt
package project
package data
package service

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.project.ModuleData
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.service.module.extension.ExternalSystemMutableModuleExtension
import consulo.externalSystem.util.{ExternalSystemConstants, Order}
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.plugins.scala.shared.util.SbtModuleType
import org.jetbrains.sbt.project.module.SbtSourceSetData

import java.util
import scala.jdk.CollectionConverters.*

/**
 * Marks the modules created for sbt source sets (`main`/`test`) with the external module type option,
 * see [[SbtSourceSetUtil.ExternalModuleTypeKey]].
 */
// upstream: ExternalSystemModulePropertyManager stores the external module type of the ModuleData
@ExtensionImpl
@Order(ExternalSystemConstants.UNORDERED)
final class SbtSourceSetModuleTypeDataService extends ScalaAbstractProjectDataService[ModuleData, Module](ProjectKeys.MODULE) {

  override def importData(toImport: util.Collection[DataNode[ModuleData]], project: Project, synchronous: Boolean): Unit = {
    val sourceSetModules = toImport.asScala.map(_.getData).collect { case data: SbtSourceSetData => data }
    if (sourceSetModules.nonEmpty) {
      SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
        for {
          data <- sourceSetModules
          module <- project.findIdeModuleOpt(data)
        } SbtModuleExtensions.modifyModel(module) { model =>
          val extension: ExternalSystemMutableModuleExtension[?] = model.getExtensionWithoutCheck(SbtProjectSystem.Id.getId)
          if (extension != null) {
            extension.setOption(SbtSourceSetUtil.ExternalModuleTypeKey, SbtModuleType.sbtSourceSetModuleType)
          }
        }
      }
    }
  }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()
}
