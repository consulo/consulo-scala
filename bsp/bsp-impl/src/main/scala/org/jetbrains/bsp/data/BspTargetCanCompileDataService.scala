package org.jetbrains.bsp.data

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.DataNode
import consulo.project.Project
import org.jetbrains.bsp.project.BspTargetCanCompile
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService

import java.util

@ExtensionImpl
class BspTargetCanCompileDataService extends ScalaAbstractProjectDataService[BspTargetCanCompileData, Project](BspTargetCanCompileData.Key) {

  override def importData(
    toImport: util.Collection[DataNode[BspTargetCanCompileData]],
    project: Project,
    synchronous: Boolean
  ): Unit = {
    // there will be always one node to import
    toImport.forEach { node =>
      BspTargetCanCompile.getInstance(project).compilableTargets = node.getData.compilableTargets
    }
  }

  override def removeData(toRemove: util.Collection[? <: Project], project: Project, synchronous: Boolean): Unit = ()
}
