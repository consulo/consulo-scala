package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.project.ModuleData
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.service.module.extension.ExternalSystemMutableModuleExtension
import consulo.externalSystem.util.{ExternalSystemApiUtil, ExternalSystemConstants, Order}
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.sbt.project.SbtProjectSystem

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
 * upstream: no counterpart. Consulo's orphan module detection (refresh of all linked projects, detach of a linked project) knows only
 * the linked paths of the module nodes which are direct children of the project node. The modules of nested module nodes
 * (the build module, the sbt subprojects, the source set modules) would be offered for removal after each reload, so their
 * linked project path is set to the one of the top level module they belong to (runs after the built-in ModuleDataService,
 * which sets the linked project path of the module data).
 */
@ExtensionImpl
@Order(ExternalSystemConstants.UNORDERED)
class SbtNestedModuleLinkedPathDataService extends ScalaAbstractProjectDataService[ModuleData, Module](ProjectKeys.MODULE) {

  override def importData(
    toImport: util.Collection[DataNode[ModuleData]],
    project: Project,
    synchronous: Boolean
  ): Unit = {
    val linkedPaths = for {
      node <- toImport.asScala.toSeq
      if node.getData.getOwner == SbtProjectSystem.Id
      topLevelNode <- topLevelModuleNode(node)
    } yield node.getData -> topLevelNode.getData.getLinkedExternalProjectPath

    if (linkedPaths.nonEmpty)
      SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
        for {
          (data, linkedPath) <- linkedPaths
          module <- project.findIdeModuleOpt(data)
          if ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.LINKED_PROJECT_PATH_KEY) != linkedPath
        } SbtModuleExtensions.modifyModel(module) { model =>
          val extension: ExternalSystemMutableModuleExtension[?] = model.getExtensionWithoutCheck(SbtProjectSystem.Id.getId)
          if (extension != null)
            extension.setOption(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY, linkedPath)
        }
      }
  }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()

  /** @return the module node which is a direct child of the project node and contains `node`, `None` if `node` is such a node */
  private def topLevelModuleNode(node: DataNode[ModuleData]): Option[DataNode[ModuleData]] =
    Iterator.iterate[DataNode[?]](node)(_.getParent)
      .takeWhile(_ != null)
      .sliding(2)
      .collectFirst {
        case Seq(moduleNode, parent) if parent.getKey == ProjectKeys.PROJECT => moduleNode
      }
      .filter(_ ne node)
      .collect {
        case moduleNode if moduleNode.getKey == ProjectKeys.MODULE => moduleNode.asInstanceOf[DataNode[ModuleData]]
      }
}
