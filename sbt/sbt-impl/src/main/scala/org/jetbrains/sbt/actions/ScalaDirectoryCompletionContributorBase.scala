package org.jetbrains.sbt.actions

import consulo.annotation.access.RequiredReadAction
import consulo.content.ContentFolderTypeProvider
import consulo.externalSystem.model.project.{ContentRootData, ModuleData}
import consulo.externalSystem.model.{DataNode, ProjectKeys, ProjectSystemId}
import consulo.externalSystem.rt.model.ExternalSystemSourceType
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.ide.action.CreateDirectoryCompletionContributor
import consulo.ide.action.CreateDirectoryCompletionContributor.Variant
import consulo.language.content.{ProductionContentFolderTypeProvider, ProductionResourceContentFolderTypeProvider, TestContentFolderTypeProvider, TestResourceContentFolderTypeProvider}
import consulo.language.psi.PsiDirectory
import consulo.module.Module
import consulo.module.content.ProjectFileIndex
import org.jetbrains.sbt.project.module.SbtSourceSetData

import java.util
import java.util.Collections.emptyList
import scala.collection.mutable
import scala.jdk.CollectionConverters.{BufferHasAsJava, CollectionHasAsScala}

/**
 * Inspired by `org.jetbrains.plugins.gradle.GradleDirectoryCompletionContributor`
 *
 * NOTE: currently there is no way to apply custom sorting to the completion items, see IDEA-306694
 */
// upstream: org.jetbrains.plugins.scala.actions.ScalaDirectoryCompletionContributorBase (scala-impl); the module data node is
// found by the subclass (the sbt and BSP lookups live in sbt-impl and bsp-impl)
abstract class ScalaDirectoryCompletionContributorBase(projectSystemId: ProjectSystemId)
  extends CreateDirectoryCompletionContributor {

  protected def findModuleData(module: Module): Option[DataNode[? <: ModuleData]]

  @RequiredReadAction
  override def getVariants(directory: PsiDirectory): util.Collection[Variant] = {
    val project = directory.getProject

    val module = ProjectFileIndex.getInstance(project).getModuleForFile(directory.getVirtualFile)
    if (module == null)
      return emptyList()

    if (!ExternalSystemApiUtil.isExternalSystemAwareModule(projectSystemId, module))
      return emptyList()

    val result = new mutable.ArrayBuffer[Variant]()

    def addAllPaths(rootData: ContentRootData, sourceType: ExternalSystemSourceType, rootType: ContentFolderTypeProvider): Seq[Variant] = {
      val paths = rootData.getPaths(sourceType)
      paths.asScala.map(p => new Variant(p.getPath, rootType)).toSeq
    }

    val contentRootsData = getModuleContentRootsData(module)
    for {
      rootData <- contentRootsData
    } {
      result ++= addAllPaths(rootData, ExternalSystemSourceType.SOURCE, ProductionContentFolderTypeProvider.getInstance)
      result ++= addAllPaths(rootData, ExternalSystemSourceType.TEST, TestContentFolderTypeProvider.getInstance)
      result ++= addAllPaths(rootData, ExternalSystemSourceType.RESOURCE, ProductionResourceContentFolderTypeProvider.getInstance)
      result ++= addAllPaths(rootData, ExternalSystemSourceType.TEST_RESOURCE, TestResourceContentFolderTypeProvider.getInstance)
    }

    result.asJava
  }

  private def getModuleContentRootsData(module: Module): Seq[ContentRootData] =
    findModuleData(module) match {
      case Some(moduleData) =>
        val sbtSourcesSetData = ExternalSystemApiUtil.findAll(moduleData, SbtSourceSetData.Key).asScala.toSeq
        val contentRoots = (sbtSourcesSetData :+ moduleData).flatMap(ExternalSystemApiUtil.findAll(_, ProjectKeys.CONTENT_ROOT).asScala)
        contentRoots.map(_.getData)
      case None =>
        Nil
    }
}
