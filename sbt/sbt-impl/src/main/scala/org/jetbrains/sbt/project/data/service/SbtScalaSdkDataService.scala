package org.jetbrains.sbt.project.data.service

import consulo.annotation.component.ExtensionImpl
import consulo.content.library.Library
import consulo.externalSystem.model.DataNode
import consulo.module.content.ModuleRootManager
import consulo.project.Project
import consulo.project.content.library.ProjectLibraryTable
import org.jetbrains.plugins.scala.project.ReplClasspath
import org.jetbrains.plugins.scala.project.external.{ScalaAbstractProjectDataService, ScalaSdkUtils}
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.data.SbtScalaSdkData

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala

@ExtensionImpl
class SbtScalaSdkDataService extends ScalaAbstractProjectDataService[SbtScalaSdkData, Library](SbtScalaSdkData.Key){

  override def importData(
    toImport: util.Collection[DataNode[SbtScalaSdkData]],
    project: Project,
    synchronous: Boolean
  ): Unit = SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
    val dataToImport = toImport.asScala
    for {
      dataNode <- dataToImport
      module <- project.getIdeModuleByNode(dataNode)
      SbtScalaSdkData(scalaVersion, scalacClasspath, scaladocExtraClasspath, compilerBridgeBinaryJar, replClasspath) = dataNode.getData
    } {
      /*
       * Reminder: SbtModuleExtData is built based on `show scalaInstance` sbt command result.
       * In theory looks like if there are no scala libraries in the module, no SbtModuleExtData should be reported for the module
       * But sbt creates `scalaInstance` in such cases anyway
       * see https://github.com/sbt/sbt/issues/6559
       * Also e.g. for Scala 3 (dotty) project, there is not explicit scala3-library dependency in modules,
       * because all modules already depend on scala3-module in the Scala3 project itself
       * So scalaInstance is reported for modules only as compiler which should be used to compile sources
       */
      scalaVersion.foreach { version =>
        val rootModel = ModuleRootManager.getInstance(module).getModifiableModel
        val projectLibrariesModel = ProjectLibraryTable.getInstance(project).getModifiableModel
        try {
          ScalaSdkUtils.configureScalaSdk(
            module,
            version,
            scalacClasspath.map(_.toPath),
            scaladocExtraClasspath.map(_.toPath),
            compilerBridgeBinaryJar.map(_.toPath),
            ReplClasspath.fromPaths(replClasspath.map(_.toPath)),
            sdkPrefix = SbtProjectSystem.Id.getReadableName.get,
            projectLibrariesModel,
            rootModel
          )
          // Consulo: Scala is enabled in a module by the Scala module extension
          SbtModuleExtensions.enableScala(rootModel)
        } finally {
          projectLibrariesModel.commit()
          rootModel.commit()
        }
      }
    }
  }

  override def removeData(toRemove: util.Collection[? <: Library], project: Project, synchronous: Boolean): Unit = ()
}
