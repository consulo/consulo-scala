package org.jetbrains.bsp.project

import consulo.annotation.component.ExtensionImpl

import com.google.gson.Gson
import consulo.externalSystem.rt.model.ExternalSystemSourceType
import consulo.externalSystem.service.project.{ExternalSystemProjectResolver, ProjectData}
import consulo.externalSystem.model.{DataNode, ProjectKeys, ProjectSystemId}
import consulo.externalSystem.task.ExternalSystemTaskManager
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.externalSystem.{ExternalSystemConfigurableAware, ExternalSystemManager}
import consulo.fileChooser.FileChooserDescriptor
import consulo.configurable.Configurable
import consulo.project.Project
import consulo.util.dataholder.{Key, UserDataHolder}
import consulo.util.lang.Pair
import consulo.virtualFileSystem.LocalFileSystem
import java.util.function.{Function, Supplier}
import org.jetbrains.bsp.*
import org.jetbrains.bsp.project.BspExternalSystemManager.DetectExternalProjectFiles
import org.jetbrains.bsp.project.importing.BspProjectResolver
import org.jetbrains.bsp.impl.protocol.BspConnectionConfig
import org.jetbrains.bsp.settings.*
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.sbt.project.ExternalSystemAutoImportAwareCompat

import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import java.util.Collections
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}

@ExtensionImpl
class BspExternalSystemManager extends ExternalSystemManager[BspProjectSettings, BspProjectSettingsListener, BspSettings, BspLocalSettings, BspExecutionSettings]
  with ExternalSystemConfigurableAware
  with ExternalSystemAutoImportAwareCompat
{

  override def getSystemId: ProjectSystemId = BSP.ProjectSystemId

  override def getSettingsProvider: Function[Project, BspSettings] = BspSettings.getInstance(_)

  override def getLocalSettingsProvider: Function[Project, BspLocalSettings] = BspLocalSettings.getInstance(_)

  override def getExecutionSettingsProvider: Function[Pair[Project, String], BspExecutionSettings] =
    pair => BspExecutionSettings.executionSettingsFor(pair.first, Path.of(pair.second))

  override def getProjectResolverFactory: Supplier[? <: ExternalSystemProjectResolver[BspExecutionSettings]] =
    () => new BspProjectResolver

  override def getTaskManagerFactory: Supplier[? <: ExternalSystemTaskManager[BspExecutionSettings]] =
    () => new BspTaskManager

  override def getExternalProjectDescriptor: FileChooserDescriptor = new BspOpenProjectDescriptor

  override def getConfigurable(project: Project): Configurable = new BspExternalSystemConfigurable(project)

  // upstream: enhanceRemoteProcessing - Consulo resolves external projects in the IDE process

  override def getAffectedExternalProjectPath(changedFileOrDirPath: String, project: Project): String = {
    if (detectExternalProjectFiles(project)) {
      val file = Path.of(changedFileOrDirPath)
      val isConfigFile = (BspConnectionConfig.isBspConfigFile(file) || BspUtil.isBloopConfigFile(file)) &&
        BspUtil.workspaces(project).contains(file.getParent)

      if (isConfigFile) file.getParent.toCanonicalPath.toString
      else null
    } else null
  }

  override def getAffectedExternalProjectFilePaths(projectPath: String, project: Project): java.util.List[Path] = {
    if (detectExternalProjectFiles(project)) {
      val workspace = Path.of(projectPath)
      val bspConfigs = BspConnectionConfig.workspaceConfigurationFiles(workspace)
      val bloopConfigs = BspUtil.bloopConfigDir(workspace).toList
        .flatMap(_.children().filter(file => file.getFileName.toString.endsWith(".json")))

      /* !!! ATTENTION !!!
      Files in `bspConfigs` and `bloopConfigs` are .json files so they won't be proceed by
      org.jetbrains.bsp.project.BspScalaFilesCrcCalculator (see BspScalaFilesCrcCalculator.isApplicable).
      `scalaCliConfigs are the only scala files that are returned for a BSP system, and they will be processed by `BspScalaFilesCrcCalculator`.
      In these files, only changes to using directives are considered when calculating the CRC value,
      which in turn determines whether to display an icon to reload the project.

      If other `.scala` files were required, and changes to the entire file (not just the using directives) needed to be considered
      when deciding whether to display the refresh icon, the existing mechanism, particularly `BspScalaFilesCrcCalculator`, would need to be adjusted.
       */
      val scalaCliConfigs =
        if (BspUtil.isBspScalaCliProject(project)) getScalaCliAffectedFiles(project, projectPath)
        else List.empty
      (bspConfigs ++ bloopConfigs ++ scalaCliConfigs).asJava
    } else {
      Collections.emptyList()
    }
  }

  private def getScalaCliAffectedFiles(project: Project, projectPath: String): List[Path] = {
    val projectNode = BspExternalSystemUtil.findProjectNode(project, projectPath)
    if (projectNode == null) return List.empty
    // upstream: cached in the user data of the project node - Consulo data nodes hold no user data
    getSourceRootFiles(projectNode)
  }

  private def getSourceRootFiles(structure: DataNode[ProjectData]): List[Path] = {
    val affectedExternalSystemSourceTypes = Seq(
      ExternalSystemSourceType.SOURCE, ExternalSystemSourceType.TEST,
      ExternalSystemSourceType.SOURCE_GENERATED, ExternalSystemSourceType.TEST_GENERATED
    )
    for {
      moduleDataNode <- ExternalSystemApiUtil.findAll(structure, ProjectKeys.MODULE).asScala.toList
      contentRootNode <- ExternalSystemApiUtil.findAll(moduleDataNode, ProjectKeys.CONTENT_ROOT).asScala.toSeq
      data = contentRootNode.getData
      sourceRoot <- affectedExternalSystemSourceTypes.flatMap(data.getPaths(_).asScala)
    } yield Path.of(sourceRoot.getPath)
  }

  private def detectExternalProjectFiles(project: Project): Boolean = {
    cached(DetectExternalProjectFiles, project) {
      if (BspUtil.isBspProject(project) && project.getBasePath != null) {
        val workspace = Path.of(project.getBasePath)
        val files = BspConnectionConfig.workspaceConfigurationFiles(workspace)
        files
          .flatMap(p => parseAsMap(p).toOption)
          .forall { details =>
            ! details.get("X-detectExternalProjectFiles")
              .contains(false)
          }
      } else true
    }
  }

  private def parseAsMap(file: Path): Try[Map[String, Any]] =
    Using(Files.newBufferedReader(file, Charset.defaultCharset())) { reader =>
      new Gson()
        .fromJson(reader, classOf[java.util.Map[String, ?]])
        .asScala
        .toMap
    }

  private def cached[A](key: Key[A], holder: UserDataHolder)(compute: => A): A = {
    Option(holder.getUserData(key)).getOrElse {
      val computed = compute
      holder.putUserData(key, computed)
      computed
    }
  }

}

object BspExternalSystemManager {
  val DetectExternalProjectFiles: Key[Boolean] = Key.create[Boolean]("BSP.detectExternalProjectFiles")

  def parseAsMap(file: Path): Map[String, Any] = {
    val virtualFile = LocalFileSystem.getInstance.findFileByNioFile(file)
    val content = new String(virtualFile.contentsToByteArray())
    new Gson().fromJson(content, classOf[java.util.Map[String, ?]]).asScala.toMap
  }
}
