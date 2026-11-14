package org.jetbrains.bsp.project

import consulo.externalSystem.model.project.ModuleData
import consulo.externalSystem.model.{DataNode, Key, ProjectKeys}
import consulo.externalSystem.service.project.ProjectData
import consulo.externalSystem.service.project.manage.ExternalProjectsManager
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.bsp.BSP
import org.jetbrains.bsp.data.{BspProjectData, SbtBuildModuleDataBsp, SbtModuleDataBsp}
import org.jetbrains.sbt.SbtUtil

import java.net.URI
import scala.jdk.CollectionConverters.*

object BspExternalSystemUtil {

  def getBspProjectData(project: Project, rootProjectPath: Option[String] = None): Option[BspProjectData] = {
    val dataEither = ExternalSystemUtil.getProjectData(project, BspProjectData.Key, rootProjectPath)
    dataEither.toSeq.flatten.headOption
  }

  private val EmptyURI = new URI("")

  def getSbtModuleData(module: Module): Option[SbtModuleDataBsp] = {
    val moduleDataSeq = getModuleData(module, SbtModuleDataBsp.Key)
    moduleDataSeq.find(_.id.uri != EmptyURI)
  }

  def getSbtBuildModuleDataBsp(module: Module): Option[SbtBuildModuleDataBsp] = {
    val moduleDataSeq = getModuleData(module, SbtBuildModuleDataBsp.Key)
    moduleDataSeq.find(_.id.uri != EmptyURI)
  }

  private def getModuleData[K](module: Module, key: Key[K]): Iterable[K] = {
    val project = module.getProject
    val moduleId = ExternalSystemApiUtil.getExternalProjectId(module) // nullable, but that's okay for use in predicate
    val rootProjectPath = Option(SbtUtil.getExternalRootProjectPath(module))
    val dataEither = ExternalSystemUtil.getModuleData(project, moduleId, key, rootProjectPath)
    //TODO: do we need to report the warning to user
    // However there is some code which doesn't expect the data to be present and just checks if it exists
    // So before reporting the warning to user we need to review usage code and decide which code expects
    // the data and which not and then probably split API into two versions: something like "get" and "getOptional"...
    dataEither.getOrElse(Nil)
  }

  // upstream: ExternalSystemApiUtil.findProjectNode
  def findProjectNode(project: Project, projectPath: String): DataNode[ProjectData] =
    getProjectDataNode(project, Some(projectPath)).toOption.orNull

  // upstream: ExternalSystemApiUtil.findChild
  def findChild[T](parent: DataNode[?], key: Key[T], predicate: DataNode[T] => Boolean): DataNode[T] =
    ExternalSystemApiUtil.findAll(parent, key).asScala.find(predicate).orNull

  /**
   * The BSP project data node of the project linked at `rootProjectPath` (the project base path by default).
   */
  def getProjectDataNode(project: Project, rootProjectPath: Option[String]): Either[String, DataNode[ProjectData]] =
    ExternalSystemUtil.getProjectDataNode(project, rootProjectPath)

  // upstream: org.jetbrains.plugins.scala.util.ExternalSystemUtil (the sbt-impl copy is private to SbtUtil); the data comes from
  // the project data cached by ExternalProjectsManager (there is no ProjectDataManager.getExternalProjectData in Consulo)
  private object ExternalSystemUtil {

    def getProjectData[K](project: Project, key: Key[K], rootProjectPath: Option[String]): Either[String, Iterable[K]] =
      getProjectDataNode(project, rootProjectPath)
        .map(ExternalSystemApiUtil.findAll(_, key).asScala.map(_.getData))

    def getModuleData[K](project: Project, moduleId: String, key: Key[K], rootProjectPath: Option[String]): Either[String, Iterable[K]] =
      getProjectDataNode(project, rootProjectPath).flatMap { projectDataNode =>
        ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE).asScala
          .find(_.getData.getId == moduleId)
          .toRight(s"can't find module data node with id `$moduleId` for project $project")
      }.map(ExternalSystemApiUtil.findAll(_, key).asScala.map(_.getData))

    def getProjectDataNode(project: Project, rootProjectPath: Option[String]): Either[String, DataNode[ProjectData]] = {
      val projectPath = rootProjectPath.getOrElse(project.getBasePath)
      val projectsData = ExternalProjectsManager.getInstance(project).getProjectData(BSP.ProjectSystemId).asScala
      val normalizedPath = ExternalSystemApiUtil.normalizePath(projectPath)
      projectsData
        .find { node =>
          val linkedPath = node.getData.getLinkedExternalProjectPath
          linkedPath == projectPath || ExternalSystemApiUtil.normalizePath(linkedPath) == normalizedPath
        }
        .toRight(s"can't find ${BSP.ProjectSystemId} external project data for project $project")
    }
  }
}
