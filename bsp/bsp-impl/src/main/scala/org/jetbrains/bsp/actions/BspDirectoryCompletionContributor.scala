package org.jetbrains.bsp.actions

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.project.ModuleData
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.localize.LocalizeValue
import consulo.module.Module
import org.jetbrains.bsp.project.BspExternalSystemUtil
import org.jetbrains.bsp.{BSP, BspBundle}
import org.jetbrains.sbt.SbtUtil
import org.jetbrains.sbt.actions.ScalaDirectoryCompletionContributorBase

import scala.jdk.CollectionConverters.CollectionHasAsScala

@ExtensionImpl
final class BspDirectoryCompletionContributor
  extends ScalaDirectoryCompletionContributorBase(BSP.ProjectSystemId) {

  override def getDescription: LocalizeValue = LocalizeValue.of(BspBundle.message("bsp.sources.set"))

  override protected def findModuleData(module: Module): Option[DataNode[? <: ModuleData]] = {
    val moduleId = ExternalSystemApiUtil.getExternalProjectId(module)
    if (moduleId == null) None
    else {
      val rootProjectPath = Option(SbtUtil.getExternalRootProjectPath(module))
      BspExternalSystemUtil.getProjectDataNode(module.getProject, rootProjectPath).toOption.flatMap { projectDataNode =>
        ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE).asScala.find(_.getData.getId == moduleId)
      }
    }
  }
}
