package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl

import com.intellij.entities.{ModuleExtensionWorkspaceEntityModifications, SharedSourcesOwnersEntityBuilder, SharedSourcesOwnersEntityModifications, SharedSourcesOwnersEntitySource}
import consulo.externalSystem.service.project.ProjectData
import consulo.externalSystem.model.{DataNode, Key}
import com.intellij.openapi.externalSystem.service.project.manage.WorkspaceDataService
import consulo.project.Project
import com.intellij.platform.workspace.jps.entities.{ModuleEntityModifications, ModuleId}
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.sbt.SbtUtil.EntityStorageOps
import org.jetbrains.sbt.WorkspaceModelUtil
import org.jetbrains.sbt.project.SharedSourcesOwnersData
import org.jetbrains.sbt.project.data.findModuleForParentOfDataNode

import java.util
import java.util.List as JList
import scala.jdk.CollectionConverters.CollectionHasAsScala

// TODO SCL-22395
@ExtensionImpl
class SharedSourcesOwnersDataWorkspaceDataService extends WorkspaceDataService[SharedSourcesOwnersData] {

  override def getTargetDataKey: Key[SharedSourcesOwnersData] = SharedSourcesOwnersData.Key

  override def importData(
    toImport: util.Collection[? <: DataNode[SharedSourcesOwnersData]],
    projectData: ProjectData,
    project: Project,
    mutableStorage: MutableEntityStorage
  ): Unit = {
    toImport.asScala.foreach { dataNode =>
      val moduleOpt = findModuleForParentOfDataNode(dataNode)
      moduleOpt.foreach { module =>
        val moduleEntityOpt = mutableStorage.resolveOpt(new ModuleId(module.getName))
        moduleEntityOpt
          .filter(WorkspaceModelUtil.findSharedSourcesOwnersEntityForModuleEntity(_, mutableStorage).isEmpty)
          .foreach { moduleEntity =>
            val sharedSourcesOwnersData = dataNode.getData
            val newEntity = createSharedSourcesOwnersEntity(sharedSourcesOwnersData.ownerModuleIds)
            ModuleEntityModifications.modifyModuleEntity(mutableStorage, moduleEntity, builder => {
              ModuleExtensionWorkspaceEntityModifications.setModuleExtensionWorkspaceEntity(builder, newEntity)
              kotlin.Unit.INSTANCE
            })
          }
      }
    }
  }

  private def createSharedSourcesOwnersEntity(ownerModulesIds: JList[String]): SharedSourcesOwnersEntityBuilder = {
    val entitySource = SharedSourcesOwnersEntitySource.INSTANCE
    SharedSourcesOwnersEntityModifications.createSharedSourcesOwnersEntity(ownerModulesIds, entitySource)
  }
}
