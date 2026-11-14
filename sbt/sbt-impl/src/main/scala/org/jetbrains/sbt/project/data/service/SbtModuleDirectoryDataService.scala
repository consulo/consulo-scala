package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.project.ModuleData
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.util.Order
import consulo.module.{Module, ModuleManager}
import consulo.project.Project
import consulo.util.io.FileUtil
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService
import org.jetbrains.sbt.project.SbtProjectSystem

import java.nio.file.{Files, Path}
import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

/**
 * Consulo's ModuleDataService creates a module with `ModuleManager.newModule(name, moduleDir)`, which returns the existing module
 * of the directory (whatever its name is), and then looks the module up by the internal name. This service runs before it and
 *  - renames the module of the directory to the internal name of the module data (e.g. the module of the preview import,
 *    named `<project>_<random number>`, or a module of a renamed sbt project), so that it is found by name;
 *  - creates the module directory if it does not exist (e.g. the `project` directory of the build module), Consulo can't load
 *    a module without its directory.
 */
@ExtensionImpl
@Order(40) // before SbtNestedModuleDataService and ExternalSystemConstants.BUILTIN_SERVICE_ORDER
class SbtModuleDirectoryDataService extends ScalaAbstractProjectDataService[ModuleData, Module](ProjectKeys.MODULE) {

  override def importData(
    toImport: util.Collection[DataNode[ModuleData]],
    project: Project,
    synchronous: Boolean
  ): Unit = {
    val modulesData = toImport.asScala.map(_.getData).filter(_.getOwner == SbtProjectSystem.Id).toSeq
    if (modulesData.nonEmpty) {
      modulesData.foreach { data =>
        val dir = Path.of(data.getModuleDirPath)
        if (!Files.exists(dir))
          Try(Files.createDirectories(dir))
      }
      SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
        renameModules(project, modulesData)
      }
    }
  }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()

  private def renameModules(project: Project, modulesData: Seq[ModuleData]): Unit = {
    val moduleManager = ModuleManager.getInstance(project)
    val modules = moduleManager.getModules.toSeq

    def moduleInDirectory(data: ModuleData): Option[Module] =
      modules.find(module => Option(module.getModuleDirPath).exists(FileUtil.pathsEqual(_, data.getModuleDirPath)))

    val renames = modulesData
      .filter(data => moduleManager.findModuleByName(data.getInternalName) == null)
      .flatMap(data => moduleInDirectory(data).map(_ -> data.getInternalName))
      .distinctBy(_._1)

    if (renames.nonEmpty) {
      val model = moduleManager.getModifiableModel
      renames.foreach { case (module, name) => model.renameModule(module, name) }
      model.commit()
    }
  }
}
