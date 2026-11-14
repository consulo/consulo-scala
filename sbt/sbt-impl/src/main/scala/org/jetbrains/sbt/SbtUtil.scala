package org.jetbrains.sbt


import consulo.application.ApplicationManager
import consulo.application.platform.PlatformTransfer
import consulo.externalSystem.model.project.ModuleData
import consulo.externalSystem.service.project.ProjectData
import consulo.externalSystem.model.{DataNode, Key, ProjectKeys, ProjectSystemId}
import consulo.externalSystem.service.project.manage.ExternalProjectsManager
import consulo.externalSystem.util.{ExternalSystemApiUtil, ExternalSystemConstants}
import consulo.http.HttpProxyManager
import consulo.logging.Logger
import consulo.module.{Module, ModuleManager}
import consulo.platform.Platform
import consulo.process.cmd.ParametersList
import consulo.project.Project
import consulo.util.io.FileUtil
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration
import org.jetbrains.plugins.scala.rt.project.Version
import org.jetbrains.sbt.Sbt.SbtModuleChildKeyInstance
import org.jetbrains.sbt.api.{SbtVersion, SbtVersionCapabilities, jarWith}
import org.jetbrains.sbt.buildinfo.BuildInfo
import org.jetbrains.sbt.project.data.{SbtBuildModuleData, SbtModuleData, SbtProjectData}
import org.jetbrains.sbt.project.settings.SbtExecutionSettings
import org.jetbrains.sbt.project.{SbtExternalSystemManager, SbtProjectSystem}
import org.jetbrains.sbt.settings.SbtSettings

import java.net.URI
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.math.Ordering.Implicits.infixOrderingOps

//noinspection ApiStatus,UnstableApiUsage
object SbtUtil {

  private lazy val log: Logger = Logger.getInstance(getClass)

  private object CommandLineOptions {
    val globalPlugins = "sbt.global.plugins"
    val globalBase = "sbt.global.base"
  }

  /**
   * The root path of the linked external project the module belongs to
   */
  // upstream: getExternalRootProjectPath
  def getExternalRootProjectPath(module: Module): String =
    ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.ROOT_PROJECT_PATH_KEY)

  def isSbtModule(module: Module): Boolean =
    ExternalSystemApiUtil.isExternalSystemAwareModule(SbtProjectSystem.Id, module)

  def isSbtProject(project: Project): Boolean = {
    val settings = sbtSettings(project)
    val linkedSettings = settings.getLinkedProjectsSettings
    !linkedSettings.isEmpty
  }

  def sbtSettings(project: Project): SbtSettings =
    ExternalSystemApiUtil.getSettings(project, SbtProjectSystem.Id).asInstanceOf[SbtSettings]

  /** Directory for global sbt plugins given sbt version */
  // upstream: @VisibleForTesting
  def globalPluginsDirectory(sbtVersion: SbtVersion, platform: Platform): Path =
    getFileProperty(CommandLineOptions.globalPlugins).getOrElse {
      val base = globalBase(sbtVersion, platform)
      base / "plugins"
    }

  /** Directory for global sbt plugins from parameters if it is explicitly set,
   * otherwise calculate from sbt version.
   */
  def globalPluginsDirectory(sbtVersion: SbtVersion, parameters: ParametersList, platform: Platform): Path = {
    val maybeCustomDir = customGlobalPluginsDirectory(parameters)
    maybeCustomDir.getOrElse {
      globalPluginsDirectory(sbtVersion, platform)
    }
  }

  private def customGlobalPluginsDirectory(parameters: ParametersList): Option[Path] = {
    val customGlobalPlugins = Option(parameters.getPropertyValue(CommandLineOptions.globalPlugins)).map(Path.of(_))
    val customGlobalBase = Option(parameters.getPropertyValue(CommandLineOptions.globalBase)).map(Path.of(_))
    val pluginsUnderCustomGlobalBase = customGlobalBase.map(_ / "plugins")
    customGlobalPlugins.orElse(pluginsUnderCustomGlobalBase)
  }

  /** Base directory for global sbt settings. */
  def globalBase(sbtVersion: SbtVersion, platform: Platform): Path = {
    val global = getFileProperty(CommandLineOptions.globalBase)
    global.getOrElse(defaultVersionedGlobalBase(sbtVersion, platform))
  }

  private def getFileProperty(name: String): Option[Path] = Option(System.getProperty(name)).flatMap { path =>
    if (path.isEmpty) None else Some(Path.of(path))
  }

  private[sbt] def defaultGlobalBase(platform: Platform): Path =
    platform.user().homePath() / Sbt.Extension

  private def defaultVersionedGlobalBase(sbtVersion: SbtVersion, platform: Platform): Path = {
    defaultGlobalBase(platform) / sbtVersion.binaryVersion.presentation
  }

  def isBuiltWithSeparateModulesForProdTest(project: Project, projectPath: Option[String] = None): Boolean = {
    val sbtProjectDataOpt = getSbtProjectData(project, projectPath)
    sbtProjectDataOpt.exists(_.prodTestSourcesSeparated)
  }

  /**
   * Checks whether the main/test modules are enabled based on
   * [[org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration.separateProdTestSources]].
   *
   * ATTENTION!
   *
   * This method returns incorrect results when an IDEA project contains multiple linked sbt projects
   * with inconsistent main/test module separation settings (i.e., some have it enabled, others disabled).
   *
   * @see [[org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration.separateProdTestSources]]
   */
  def hasScalaCompilerSeparateProdTestSourcesEnabled(project: Project): Boolean =
    ScalaCompilerConfiguration.instanceIn(project).separateProdTestSources

  /**
   * Determines whether the [[SbtProjectData]] for the external system project at `projectPath` corresponds to a preview mode.
   *
   * @return `true` if the sbt project is in preview mode or if [[SbtProjectData]] cannot be found; `false` otherwise.
   *         If [[SbtProjectData]] is not found at `projectPath`, the external system project at given `projectPath` is likely not an sbt project.
   */
  def isPreview(project: Project, projectPath: String): Boolean = {
    val sbtProjectDataOpt = getSbtProjectData(project, Some(projectPath))
    sbtProjectDataOpt.forall(_.isPreview)
  }

  def getSbtModuleDataNode(module: Module): Option[DataNode[? <: ModuleData]] = {
    val moduleId = Option(ExternalSystemApiUtil.getExternalProjectId(module))
    moduleId.flatMap { id =>
      val project = module.getProject
      val rootProjectPath = Option(getExternalRootProjectPath(module))
      ExternalSystemUtil.getModuleDataNode(SbtProjectSystem.Id, project, id, rootProjectPath, Some(SbtModuleChildKeyInstance)).toOption
    }
  }

  def structurePluginBinaryVersion(sbtVersion: SbtVersion): Version = {
    if (sbtVersion.isSbt2)
      Version("2")
    else if (sbtVersion >= SbtVersion("1.3.0"))
      Version("1.3")
    else if (sbtVersion.value.major(1) >= Version("1"))
      Version("1.0")
    else
      sbtVersion.value.major(2) //effectively ~ 0.13
  }

  /**
   * Returns the sbt settings (as individual lines) that add the sbt-structure plugin from the local Scala plugin repository.
   */
  private[sbt] def sbtStructurePluginDeclaration(sbtVersion: SbtVersion, repoDir: Path): Seq[String] = {
    val sbtStructurePluginBinVersion = structurePluginBinaryVersion(sbtVersion)
    Seq(
      s"""resolvers += MavenCache("Scala Plugin Bundled Repository", file(raw"${repoDir.normalizedLocalPath}"))""",
      s"""addSbtPlugin("org.jetbrains.scala" % "sbt-structure-extractor" % "${BuildInfo.sbtStructureVersion}", "$sbtStructurePluginBinVersion")"""
    )
  }

  /** The registry key which enables measuring the sbt import time. */
  private[sbt] final val ImportTimeMeasurementRegistryKey = "sbt.import.time.measurement"

  private val SbtTaskTimingsPluginVersion = "0.0.5"

  /**
   * Returns the sbt setting that adds the `sbt-task-timings` plugin, which measures the import time.
   *
   * Unlike `sbt-structure-extractor`, this plugin is not bundled in the Scala plugin.
   * It has to be downloaded from the internet.
   *
   * @see [[https://github.com/JetBrains/sbt-task-timings]]
   */
  private[sbt] def sbtTaskTimingsPluginDeclaration: String =
    s"""addSbtPlugin("org.jetbrains.scala" % "sbt-task-timings" % "$SbtTaskTimingsPluginVersion")"""

  def detectSbtVersion(project: Project): SbtVersion = {
    val workingDirPath = getWorkingDirPath(project)
    val workingDir = Path.of(workingDirPath)
    val sbtSettings = SbtExternalSystemManager.executionSettingsFor(project, workingDirPath)
    val launcher = SbtUtil.getLauncherJar(sbtSettings)
    SbtUtil.detectSbtVersion(workingDir, launcher)
  }

  def detectSbtVersion(projectRoot: Path, sbtLauncher: => Path): SbtVersion =
    SbtVersionDetector.detectSbtVersion(projectRoot, sbtLauncher)

  def getSbtModuleData(module: Module): Option[SbtModuleData] = {
    val project = module.getProject
    getSbtModuleData(project, module)
  }

  def getSbtModuleData(project: Project, module: Module): Option[SbtModuleData] = {
    val emptyURI = new URI("")

    val moduleDataSeq = getSbtModuleData(project, module, SbtModuleData.Key)
    moduleDataSeq.find(_.buildURI.uri != emptyURI)
  }

  def getSbtModuleData(project: Project, moduleId: String, rootProjectPath: String): Option[SbtModuleData] = {
    val emptyURI = new URI("")

    val moduleDataSeq = getSbtModuleData(project, moduleId, Some(rootProjectPath), SbtModuleData.Key)
    moduleDataSeq.find(_.buildURI.uri != emptyURI)
  }

  def getBuildModuleData(project: Project, module: Module): Option[SbtBuildModuleData] = {
    val emptyURI = new URI("")

    val moduleDataSeq = getSbtModuleData(project, module, SbtBuildModuleData.Key)
    moduleDataSeq.find(_.buildFor.uri != emptyURI)
  }

  def getSbtModuleData[K](project: Project, module: Module, key: Key[K]): Iterable[K] = {
    val moduleId = ExternalSystemApiUtil.getExternalProjectId(module)
    val rootProjectPath = Option(getExternalRootProjectPath(module))
    getSbtModuleData(project, moduleId, rootProjectPath, key)
  }

  def getSbtModuleData[K](project: Project, moduleId: String, rootProjectPath: Option[String], key: Key[K]): Iterable[K] = {
    if (moduleId == null) return Nil
    val dataEither = ExternalSystemUtil.getModuleData(SbtProjectSystem.Id, project, moduleId, key, rootProjectPath, Some(SbtModuleChildKeyInstance))
    //TODO: do we need to report the warning to user
    // However there is some code which doesn't expect the data to be present and just checks if it exists
    // So before reporting the warning to user we need to review usage code and decide which code expects
    // the data and which not and then probably split API into two versions: something like "get" and "getOptional"...
    dataEither.getOrElse(Nil)
  }

  case class SbtProjectUriAndId(uri: String, id: String)

  def getSbtProjectUriAndId(module: Module): Option[SbtProjectUriAndId] = {
    val moduleData = getSbtModuleData(module)
    moduleData.map { data =>
      SbtProjectUriAndId(data.buildURI.toString, data.id)
    }
  }

  def makeSbtProjectId(data: SbtModuleData): String = {
    val uri = data.buildURI
    val id = data.id
    s"{$uri}$id"
  }

  private def getLauncherDir: Path = getDirInPlugin("launcher")

  /**
   * Returns the plugin repo directory, transferring it to the remote environment if needed.
   * When transferred, all subdirectories and their jars are migrated as well.
   *
   * During project import, prefer using the repo dir from the import context to avoid redundant copying of multiple jars
   * (but in practice, it shouldn't happen due to caching).
   * In other contexts (e.g., generating managed sources), if the repo dir was already transferred, it should return the same
   * temporary directory thanks to caching. Otherwise, it will transfer again.
   *
   * The returned path is not local in the context of the remote environment and may contain the "remote label".
   * If necessary, normalize the path before usage.
   */
  def getRepoDir(platform: Platform): Path =
    PlatformTransfer.getInstance().transfer(getDirInPlugin("repo"), platform)

  def getSbtStructureJar(sbtVersion: SbtVersion, repoDir: Path): Option[Path] = {
    val binVersion = structurePluginBinaryVersion(sbtVersion)
    val structurePath =
      if (binVersion ~= Version("2"))
        Some(BuildInfo.sbtStructurePath_2)
      else if (binVersion ~= Version("1.3"))
        Some(BuildInfo.sbtStructurePath_1_3)
      else if (binVersion ~= Version("1.0"))
        Some(BuildInfo.sbtStructurePath_1_0)
      else if (binVersion ~= Version("0.13"))
        Some(BuildInfo.sbtStructurePath_0_13)
      else
        None

    structurePath.map { relativePath =>
      repoDir / relativePath
    }
  }

  def defaultLauncherPath: Path = getLauncherDir / "sbt-launch.jar"

  def getLauncherJar(settings: SbtExecutionSettings): Path =
    settings.customLauncher.map(_.toPath).getOrElse(defaultLauncherPath)

  /** Normalizes pathname so that backslashes don't get interpreted as escape characters in interpolated strings. */
  def normalizePath(path: Path): String = path.toCanonicalPath.toString.replace('\\', '/')

  private def pluginBase: Path = {
    val path = jarWith[this.type]
    if (path.getFileName.toString == "classes") path.getParent
    else path.getParent.getParent
  }

  private def getSbtProjectData(project: Project, rootProjectPath: Option[String] = None): Option[SbtProjectData] = {
    val dataEither = ExternalSystemUtil.getProjectData(SbtProjectSystem.Id, project, SbtProjectData.Key, rootProjectPath)
    dataEither.toSeq.flatten.headOption
  }

  private def getDirInPlugin(dirName: String): Path = {
    val res = pluginBase / dirName
    if (!res.exists && isInTest) {
      val start = Option(jarWith[this.type].getParent)
      start.flatMap(findDirInPlugin(_, dirName))
        .getOrElse(throw new RuntimeException(s"could not find dir $dirName at or above ${start.get}"))
    }
    else res
  }

  private def findDirInPlugin(from: Path, dirName: String): Option[Path] = {
    val dir = from / "target" / "plugin" / "Scala" / dirName
    if (dir.isDirectory) Option(dir)
    else Option(from.getParent).flatMap(findDirInPlugin(_, dirName))
  }

  private def isInTest: Boolean = ApplicationManager.getApplication.isUnitTestMode

  def sbtVersionParam(sbtVersion: SbtVersion): String =
    s"-Dsbt.version=$sbtVersion"

  def getStaticProxyConfigurationJvmOptions: Map[String, String] =
    HttpProxyManager.getInstance().getJvmProperties(false, null).asScala.map(p => p.getFirst -> p.getSecond).toMap

  def getWorkingDirPath(project: Project): String =
    getWorkingDirPathOpt(project)
      .getOrElse(throw new IllegalStateException(s"no project directory found for project ${project.getName}"))

  /**
   * @note the method can return [[None]] for example in tests,
   *       when the test project doesn't yet have modules and the dir can't be guessed
   */
  def getWorkingDirPathOpt(project: Project): Option[String] = {
    //Fist try to calculate root path based on `getExternalRootProjectPath`
    //When sbt project reference another sbt project via `RootProject` this will correctly find the root project path (see SCL-21143)
    //However, if user manually linked multiple SBT projects via external system tool window (sbt tool window)
    //using "Link sbt Project" button (the one with "plus" icon), it  will randomly choose one of the projects
    // TODO - think about some possibility to allow the user to choose in which project the shell should be fired
    val externalRootProjectPath: Option[String] = {
      val modules = ModuleManager.getInstance(project).getModules.toSeq
      modules.iterator.map(getExternalRootProjectPath).find(_ != null)
    }
    externalRootProjectPath
      .orElse {
        // externalRootProjectPath can be empty when an IDEA project has not yet been "linked" to an external project.
        // In other words, the project has not yet been imported as a project from some build tool. For example, an
        // sbt project on disk can be opened in IDEA before the Scala plugin is even installed and enabled.
        // After the Scala plugin is installed, the project will initially have an empty `externalRootProjectPath` until
        // it is imported as an sbt project using the external system machinery.
        val message = s"Can't calculate external root project path for project `${project.getName}`, fallback to project base dir"
        if (!isInTest)
          log.warn(message)
        val guessedDir = project.getBaseDir
        Option(guessedDir).map(_.getCanonicalPath)
      }
  }

  /**
   * @return path of the directory containing IntelliJ module files (~ `./.idea/modules`)
   */
  def getDefaultModuleFilesDirectory(projectRoot: Path): String =
    (projectRoot / Sbt.ModulesDirectory).toCanonicalPath.toString

  // NOTE: "*/*" syntax is deprecated since sbt 1.1 and doesn't work in sbt 2
  def sbtStructureGlobalCommand(command: String, sbtVersion: SbtVersion): String =
    if (SbtVersionCapabilities.isSlashSyntaxSupported(sbtVersion))
      s"Global / $command"
    else
      s"*/*:$command"

  /**
   * Creates a temporary `.sbt` file with the given content on the given platform.
   */
  // upstream: @RequiresBackgroundThread
  def createTemporarySbtFile(content: String, platform: Platform, projectOpt: Option[Project]): Path = {
    // upstream: EelProjectUtils.createTemporaryFile(project, ...) when the project is given
    val tmpPluginsSbtFile = platform.fs().createTempFile("idea", Sbt.Extension)

    Files.writeString(tmpPluginsSbtFile, content)
    Runtime.getRuntime.addShutdownHook(Thread(() => FileUtil.delete(tmpPluginsSbtFile): Unit))
    tmpPluginsSbtFile
  }

  /**
   * Class that holds keys corresponding to the types of child modules added to the ModuleData during the SBT import process.
   */
  case class SbtModuleChildKey(sbtNestedModuleKey: Key[? <: ModuleData], sbtSourceSetModuleKey: Key[? <: ModuleData])

  // upstream: org.jetbrains.plugins.scala.util.ExternalSystemUtil (scala-api); the data comes from the project data
  // cached by ExternalProjectsManager (there is no ProjectDataManager.getExternalProjectData in Consulo)
  private object ExternalSystemUtil {

    def getProjectData[K](
      projectSystemId: ProjectSystemId,
      project: Project,
      key: Key[K],
      rootProjectPath: Option[String]
    ): Either[String, Iterable[K]] =
      getProjectDataNode(projectSystemId, project, rootProjectPath.orElse(Option(project.getBasePath)))
        .map(ExternalSystemApiUtil.findAll(_, key).asScala.map(_.getData))

    def getModuleData[K](
      projectSystemId: ProjectSystemId,
      project: Project,
      moduleId: String,
      key: Key[K],
      rootProjectPath: Option[String],
      sbtModuleChildKey: Option[SbtModuleChildKey]
    ): Either[String, Iterable[K]] =
      getModuleDataNode(projectSystemId, project, moduleId, rootProjectPath, sbtModuleChildKey)
        .map(ExternalSystemApiUtil.findAll(_, key).asScala.map(_.getData))

    def getModuleDataNode(
      projectSystemId: ProjectSystemId,
      project: Project,
      moduleId: String,
      rootProjectPath: Option[String],
      sbtModuleChildKey: Option[SbtModuleChildKey]
    ): Either[String, DataNode[? <: ModuleData]] = {
      val projectDataNode = getProjectDataNode(projectSystemId, project, rootProjectPath) match {
        case Right(value) => value
        case Left(error) =>
          return Left(error)
      }

      val moduleDataNode = sbtModuleChildKey match {
        case Some(x) => processSbtModuleDataChildNodes(projectDataNode, moduleId, x)
        case _ => findModuleDataInChildren(moduleId, ProjectKeys.MODULE, Seq(projectDataNode)).toOption
      }
      moduleDataNode.toRight(s"can't find module data node with id `$moduleId` for project $project")
    }

    private def findModuleDataInChildren(
      moduleId: String,
      key: Key[? <: ModuleData],
      parents: Seq[DataNode[?]]
    ): Either[Seq[DataNode[? <: ModuleData]], DataNode[? <: ModuleData]] = {
      val children: Seq[DataNode[? <: ModuleData]] = parents.flatMap(ExternalSystemApiUtil.findAll(_, key).asScala.toSeq)
      children.find(_.getData.getId == moduleId).toRight(children)
    }

    private def processSbtModuleDataChildNodes(
      projectDataNode: DataNode[ProjectData],
      moduleId: String,
      moduleChildKey: SbtModuleChildKey
    ): Option[DataNode[? <: ModuleData]] = {
      val moduleDataChildren = findModuleDataInChildren(moduleId, ProjectKeys.MODULE, Seq(projectDataNode)) match {
        case Right(r) => return Some(r)
        case Left(children) => children
      }

      val sbtNestedModuleDataChildren = findModuleDataInChildren(moduleId, moduleChildKey.sbtNestedModuleKey, moduleDataChildren) match {
        case Right(r) => return Some(r)
        // it is required to add moduleDataChildren to sbtNestedModuleChildren, because sbt source set modules may be found in both
        case Left(children) => children ++ moduleDataChildren
      }

      findModuleDataInChildren(moduleId, moduleChildKey.sbtSourceSetModuleKey, sbtNestedModuleDataChildren).toOption
    }

    private def getProjectDataNode(
      projectSystemId: ProjectSystemId,
      project: Project,
      rootProjectPath: Option[String]
    ): Either[String, DataNode[ProjectData]] = {
      val projectPath = rootProjectPath.getOrElse(project.getBasePath)
      val projectsData = ExternalProjectsManager.getInstance(project).getProjectData(projectSystemId).asScala
      val normalizedPath = ExternalSystemApiUtil.normalizePath(projectPath)
      projectsData
        .find { node =>
          val linkedPath = node.getData.getLinkedExternalProjectPath
          linkedPath == projectPath || ExternalSystemApiUtil.normalizePath(linkedPath) == normalizedPath
        }
        .orElse {
          // in tests `project.getBasePath` doesn't equal to actual external project data
          if (ApplicationManager.getApplication.isUnitTestMode)
            projectsData.find(_.getData.getInternalName == project.getName)
          else None
        }
        .toRight(s"can't find $projectSystemId external project data for project $project")
    }
  }
}
