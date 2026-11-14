package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.DataNode
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.sbt.project.data.{DisplayModuleNameData, findModuleForParentOfDataNode}
import org.jetbrains.sbt.project.settings.DisplayModuleName

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala

@ExtensionImpl
class DisplayModuleNameDataService extends ScalaAbstractProjectDataService[DisplayModuleNameData, Module](DisplayModuleNameData.Key) {

  override def importData(
    toImport: util.Collection[DataNode[DisplayModuleNameData]],
    project: Project,
    synchronous: Boolean
  ): Unit = SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
    toImport.asScala.foreach { dataNode =>
      val parentModuleOpt = findModuleForParentOfDataNode(dataNode)(project)
      parentModuleOpt match {
        case Some(module) =>
          val displayModuleName = DisplayModuleName.getInstance(module)
          val displayName = dataNode.getData.name
          displayModuleName.setName(displayName)
        case _ =>
      }
    }
  }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()
}
