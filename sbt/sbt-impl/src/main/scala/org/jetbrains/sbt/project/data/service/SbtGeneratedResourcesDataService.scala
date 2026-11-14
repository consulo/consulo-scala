package org.jetbrains.sbt
package project
package data
package service

import consulo.annotation.component.ExtensionImpl
import consulo.content.base.GeneratedContentFolderPropertyProvider
import consulo.externalSystem.model.DataNode
import consulo.externalSystem.util.{ExternalSystemConstants, Order}
import consulo.content.ContentFolderTypeProvider
import consulo.language.content.{LanguageContentFolderScopes, ProductionResourceContentFolderTypeProvider, TestResourceContentFolderTypeProvider}
import consulo.module.Module
import consulo.project.Project
import consulo.util.io.FileUtil
import consulo.virtualFileSystem.util.VirtualFileUtil
import org.jetbrains.plugins.scala.project.external.ScalaAbstractProjectDataService

import java.util
import scala.jdk.CollectionConverters.*

// upstream: ContentRootDataService imports RESOURCE_GENERATED / TEST_RESOURCE_GENERATED source types; Consulo imports managed
// resource directories as plain resources, this service runs after it and marks them generated
@ExtensionImpl
@Order(ExternalSystemConstants.UNORDERED)
final class SbtGeneratedResourcesDataService extends ScalaAbstractProjectDataService[SbtGeneratedResourcesData, Module](SbtGeneratedResourcesData.Key) {

  override def importData(
    toImport: util.Collection[DataNode[SbtGeneratedResourcesData]],
    project: Project,
    synchronous: Boolean
  ): Unit = SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
    val modulePaths = toImport.asScala.toSeq.flatMap { dataNode =>
      project.getIdeModuleByNode(dataNode).map(_ -> dataNode.getData.paths.asScala.map(FileUtil.toSystemIndependentName).toSet)
    }
    modulePaths.groupMapReduce(_._1)(_._2)(_ ++ _).foreach { case (module, paths) =>
      SbtModuleExtensions.modifyModel(module) { model =>
        for {
          contentEntry <- model.getContentEntries
          folder <- contentEntry.getFolders(ResourceFolders)
          if paths.contains(FileUtil.toSystemIndependentName(VirtualFileUtil.urlToPath(folder.getUrl)))
        } folder.setPropertyValue(GeneratedContentFolderPropertyProvider.IS_GENERATED, java.lang.Boolean.TRUE)
      }
    }
  }

  private val ResourceFolders: java.util.function.Predicate[ContentFolderTypeProvider] =
    LanguageContentFolderScopes.of(ProductionResourceContentFolderTypeProvider.getInstance())
      .or(LanguageContentFolderScopes.of(TestResourceContentFolderTypeProvider.getInstance()))

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()
}
