package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl

import consulo.externalSystem.service.project.ProjectData
import consulo.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.sbt.api.RichSeq
import org.jetbrains.sbt.project.SharedSourcesOwnersData
import org.jetbrains.sbt.project.data.findModuleForParentOfDataNode
import org.jetbrains.sbt.project.settings.SharedSourcesOwnerModules

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala

// TODO SCL-22395
@ExtensionImpl
class SharedSourcesOwnersDataService extends ScalaAbstractProjectDataService[SharedSourcesOwnersData, Module](SharedSourcesOwnersData.Key){

  override def importData(
    toImport: util.Collection[? <: DataNode[SharedSourcesOwnersData]],
    projectData: ProjectData,
    project: Project,
    modelsProvider: IdeModifiableModelsProvider
  ): Unit = {
    val projectIdToModuleName = getProjectIdToActualModuleNameMap(modelsProvider)
    clearOwnerModules(modelsProvider.getModules)
    toImport.asScala.foreach { sharedSourcesOwnersNode =>
      val ownersModuleNames = sharedSourcesOwnersNode.getData.ownerModuleIds
      val actualOwnerModuleNames = ownersModuleNames.asScala.map(projectIdToModuleName).toSeq
      if (actualOwnerModuleNames.nonEmpty) {
        val parentModule = findModuleForParentOfDataNode(sharedSourcesOwnersNode)
        parentModule.foreach(updateSharedSourcesOwnerModules(_, actualOwnerModuleNames))
      }
    }
    super.importData(toImport, projectData, project, modelsProvider)
  }

  private def clearOwnerModules(modules: Array[Module]): Unit =
    modules.foreach { module =>
      val sharedSourcesOwnerModules = SharedSourcesOwnerModules.getInstance(module)
      val actualOwnersModuleNames = sharedSourcesOwnerModules.getState.ownersModuleNames
      if (actualOwnersModuleNames != null) {
        sharedSourcesOwnerModules.getState.ownersModuleNames = null
      }
    }

  private def updateSharedSourcesOwnerModules(module: Module, ownerModuleNames: Seq[String]): Unit = {
    val sharedSourcesOwnerModules = SharedSourcesOwnerModules.getInstance(module)
    sharedSourcesOwnerModules.getState.ownersModuleNames = ownerModuleNames.toJavaList
  }

  private def getProjectIdToActualModuleNameMap(modelsProvider: IdeModifiableModelsProvider): Map[String, String] =
    modelsProvider.getModules.flatMap { module =>
      val externalProjectId = ExternalSystemApiUtil.getExternalProjectId(module)
      if (externalProjectId == null) None
      else {
        val actualModuleName = modelsProvider.getModifiableModuleModel.getActualName(module)
        Some(externalProjectId -> actualModuleName)
      }
    }.toMap
}
