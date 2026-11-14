package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.project.ModuleData
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.util.{ExternalSystemApiUtil, ExternalSystemConstants, Order}
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.sbt.project.module.SbtNestedModuleData
import org.jetbrains.sbt.settings.SbtSettings

import java.util
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SetHasAsJava}

/**
 * upstream: creates the modules of [[SbtNestedModuleData]] (and of SbtSourceSetData in SbtSourceSetDataService) and prefixes
 * their names with the actual name of the parent module. Consulo's external system creates modules only for
 * [[ProjectKeys.MODULE]] nodes, so the nested and source set module nodes use that key and the built-in ModuleDataService creates them
 * (with the names computed by the resolver).
 *
 * The service runs before the built-in ModuleDataService, so that the module data nodes are deserialized with the class loader
 * of this plugin (they may contain [[SbtNestedModuleData]] and SbtSourceSetData).
 */
@ExtensionImpl
@Order(41) // ExternalSystemConstants.BUILTIN_SERVICE_ORDER - 1
class SbtNestedModuleDataService extends ScalaAbstractProjectDataService[ModuleData, Module](ProjectKeys.MODULE) {

  override def importData(
    toImport: util.Collection[DataNode[ModuleData]],
    project: Project,
    synchronous: Boolean
  ): Unit = {
    val sbtNestedModules = toImport.asScala.toList.filter(_.getData.isInstanceOf[SbtNestedModuleData])
    if (sbtNestedModules.nonEmpty) {
      val rootProjectPath = Option(ExternalSystemApiUtil.findParent(sbtNestedModules.head, ProjectKeys.PROJECT))
        .map(_.getData.getLinkedExternalProjectPath)
      rootProjectPath.foreach(setModulesInExternalSystemSettings(project, _, sbtNestedModules))
    }
  }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()

  //note: before introducing SbtNestedModuleData setting modules in ExternalSystemResolveProjectTask.doExecute was enough, but because
  // the logic there does not take into account modules with keys different than ProjectKeys.MODULE it was needed to implement it on our own for SbtNestedModuleData.
  private def setModulesInExternalSystemSettings(
    project: Project,
    rootProjectPath: String,
    sbtNestedModules: List[DataNode[ModuleData]]
  ): Unit = {
    val linkedProjectSettings = SbtSettings.getInstance(project).getLinkedProjectSettings(rootProjectPath)
    if (linkedProjectSettings != null) {
      val sbtNestedModulePaths = sbtNestedModules.map(_.getData).map(_.getLinkedExternalProjectPath)

      val externalModulePaths = (sbtNestedModulePaths ++ linkedProjectSettings.getModules.asScala.toSeq).toSet
      linkedProjectSettings.setModules(externalModulePaths.asJava)
    }
  }
}
