package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl

import consulo.externalSystem.model.DataNode
import consulo.externalSystem.service.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.sbt.project.data.SbtPlay2ProjectData

import java.util

@ExtensionImpl
class Play2StubDataService extends ScalaAbstractProjectDataService[SbtPlay2ProjectData, Project](SbtPlay2ProjectData.Key) {

  override def importData(
    toImport: util.Collection[? <: DataNode[SbtPlay2ProjectData]],
    projectData: ProjectData,
    project: Project,
    modelsProvider: IdeModifiableModelsProvider
  ): Unit = ()
}
