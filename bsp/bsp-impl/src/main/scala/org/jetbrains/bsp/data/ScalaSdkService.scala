package org.jetbrains.bsp.data

import consulo.annotation.component.ExtensionImpl
import consulo.content.library.Library
import consulo.externalSystem.model.DataNode
import consulo.module.Module
import consulo.module.content.ModuleRootManager
import consulo.project.Project
import consulo.project.content.library.ProjectLibraryTable
import org.jetbrains.bsp.BSP
import org.jetbrains.plugins.scala.project.*
import org.jetbrains.plugins.scala.api.project.ScalaLanguageLevel
import org.jetbrains.plugins.scala.project.external.{ReplClasspathCachedResolver, ScalaAbstractProjectDataService, ScalaSdkUtils}

import java.nio.file.Path
import java.util

@ExtensionImpl // upstream: order="last" (data services are grouped by data key in Consulo)
class ScalaSdkService extends ScalaAbstractProjectDataService[ScalaSdkData, Library](ScalaSdkData.Key) {

  override final def importData(
    toImport: java.util.Collection[DataNode[ScalaSdkData]],
    project: Project,
    synchronous: Boolean
  ): Unit = BspModuleExtensions.executeProjectChangeAction(project, synchronous) {
    //noinspection ApiStatus
    val replClasspathResolver = new ReplClasspathCachedResolver(project)
    toImport.forEach(doImport(_, project, replClasspathResolver.resolve))
  }

  override def removeData(toRemove: util.Collection[? <: Library], project: Project, synchronous: Boolean): Unit = ()

  private def doImport(dataNode: DataNode[ScalaSdkData], project: Project, resolveReplClasspath: String => ReplClasspath): Unit =
    for {
      module <- project.getIdeModuleByNode(dataNode)
    } {
      val ScalaSdkData(_, scalaVersion, scalacClasspath, _, scalacOptions) = dataNode.getData
      module.configureScalaCompilerSettingsFrom("bsp", scalacOptions, project)
      configureScalaSdk(
        project,
        module,
        scalaVersion,
        scalacClasspath.map(_.toPath),
        resolveReplClasspath
      )
    }

  private def configureScalaSdk(
    project: Project,
    module: Module,
    scalaVersionOpt: Option[String],
    compilerClasspath: Seq[Path],
    resolveReplClasspath: String => ReplClasspath
  ): Unit = for {
    scalaVersion <- scalaVersionOpt
    if ScalaLanguageLevel.findByVersion(scalaVersion).isDefined
  } {
    val compilerBridgeBinaryJar = ScalaSdkUtils.resolveCompilerBridgeJar(project, scalaVersion)
    val replClasspath = resolveReplClasspath(scalaVersion)
    val rootModel = ModuleRootManager.getInstance(module).getModifiableModel
    val projectLibrariesModel = ProjectLibraryTable.getInstance(project).getModifiableModel
    try {
      ScalaSdkUtils.configureScalaSdk(
        module,
        scalaVersion,
        compilerClasspath,
        // TODO: currently we agreed that BSP implementation should just omit Scala3 doc jars in `ScalaBuildTarget.jars` field
        //  and we should probably create a separate request to obtain scaladoc classpath
        //  see https://github.com/build-server-protocol/build-server-protocol/issues/229
        scaladocExtraClasspath = Nil,
        compilerBridgeBinaryJar = compilerBridgeBinaryJar,
        replClasspath = replClasspath,
        BSP.Name,
        projectLibrariesModel,
        rootModel
      )
      // Consulo: Scala is enabled in a module by the Scala module extension
      BspModuleExtensions.enableScala(rootModel)
    } finally {
      projectLibrariesModel.commit()
      rootModel.commit()
    }
  }
}
