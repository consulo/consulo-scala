package org.jetbrains.bsp.project.importing

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.importing.{AbstractExternalModuleImportProvider, ExternalModuleImportContext, SelectExternalProjectStep}
import consulo.externalSystem.model.DataNode
import consulo.externalSystem.service.project.ProjectData
import consulo.externalSystem.service.project.manage.ProjectDataManager
import consulo.project.Project
import consulo.ui.ex.wizard.WizardStep
import consulo.ui.image.Image
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.Inject
import org.jetbrains.bsp.*
import org.jetbrains.bsp.project.importing.bspConfigSteps.*
import org.jetbrains.bsp.project.importing.setup.BspSetupProvider
import org.jetbrains.bsp.impl.protocol.BspConnectionConfig
import org.jetbrains.bsp.settings.*
import org.jetbrains.bsp.settings.BspProjectSettings.*
import org.jetbrains.bsp.settings.PreImportConfig.*
import org.jetbrains.sbt.platform
import org.jetbrains.sbt.project.SbtProjectImportProvider

import java.io.File
import java.nio.file.{Files, Path}
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * Imports a BSP workspace (a `.bsp` connection file, a Bloop configuration, a Mill or Scala CLI project, or an sbt project
 * imported through BSP).
 *
 * upstream: AbstractExternalProjectImportProvider + BspProjectImportBuilder (AbstractExternalProjectImportBuilder) with the
 * BspSetupConfigStep/BspChooseConfigStep wizard steps; the builder state (pre-import and server config, whether the connection
 * file was generated) is kept in the imported [[BspProjectSettings]]. BspProjectOpenProcessor and the opening flow of BspOpenProjectProvider (opening a
 * BSP workspace directly, linking to an existing project, the selective import dialog) - opening a directory as a new project
 * is done by this import provider in Consulo.
 */
@ExtensionImpl
final class BspProjectImportProvider @Inject()(dataManager: ProjectDataManager)
  extends AbstractExternalModuleImportProvider(dataManager, BSP.ProjectSystemId) {

  override def getIcon: Image = BSP.Icon

  override def canImport(fileOrDirectory: Path): Boolean = {
    val workspace = if (Files.isDirectory(fileOrDirectory)) fileOrDirectory else fileOrDirectory.getParent
    workspace != null && (BspProjectOpenProcessor.canOpenProject(workspace) || SbtProjectImportProvider.canImport(fileOrDirectory))
  }

  override def getPathToBeImported(file: Path): String =
    (if (Files.isDirectory(file)) file else file.getParent).toString

  override protected def doPrepare(context: ExternalModuleImportContext): Unit = {}

  override protected def beforeCommit(dataNode: DataNode[ProjectData], project: Project): Unit = {}

  override protected def getExternalProjectConfigToUse(file: File): File = file

  override protected def applyExtraSettings(context: ExternalModuleImportContext): Unit = {}

  /**
   * The wizard system reuses the builder between different runs of the wizard (IDEA-246371),
   * so we need to manually reset on every run. On this occasion, we can preconfigure any
   * data that can be autodetected before running the wizard.
   */
  // upstream: BspProjectImportBuilder.reset/autoConfigure in createSteps - the workspace is known only after the first step
  override def ensureProjectIsDefined(context: ExternalModuleImportContext): CompletableFuture[?] = {
    context.getProjectSettings match {
      case settings: BspProjectSettings if settings.getExternalProjectPath != null =>
        settings.preImportConfig = AutoPreImport
        settings.serverConfig = AutoConfig
        settings.bspConfigGenerated = false
        val workspace = Path.of(settings.getExternalProjectPath)
        val configSetups = bspConfigSteps.configSetupChoices(workspace)
        if (configSetups.size == 1)
          BspJdkUtil.findOrCreateBestJdkForProject(None, workspace.platform)
            .foreach(bspConfigSteps.configureBuilder(_, settings, workspace, configSetups.head))
      case _ =>
    }
    super.ensureProjectIsDefined(context)
  }

  override def buildSteps(consumer: Consumer[WizardStep[ExternalModuleImportContext]], context: ExternalModuleImportContext): Unit = {
    super.buildSteps(consumer, context)
    consumer.accept(new BspSetupConfigStep)
    consumer.accept(new BspChooseConfigStep)
  }
}

object BspProjectOpenProcessor {

  /**
   * Checks whether the given workspace directory belongs to the BSP external system.
   *
   * Any changes to the logic of this method should also be reflected in
   * [[org.jetbrains.bsp.project.BspUnlinkedProjectAware#isBspBuildFile]],
   * as both methods are responsible for determining whether a workspace (or a file within it) belongs to the BSP.
   */
  def canOpenProject(workspace: VirtualFile): Boolean =
    canOpenProject(workspace.toNioPath)

  // upstream: sbt projects in Docker (Eel) containers are opened as BSP (SCL-17359) - Consulo has no Docker Eel descriptors
  def canOpenProject(ioWorkspace: Path): Boolean =
    hasBspConfiguration(ioWorkspace) || isScalaCliOrMill(ioWorkspace)

  private[bsp] def hasBspConfiguration(workspace: Path): Boolean = {
    val bspConnectionProtocolSupported = BspConnectionConfig.workspaceConfigurationFiles(workspace).nonEmpty
    val bloopProject = BspUtil.bloopConfigDir(workspace).isDefined
    bspConnectionProtocolSupported || bloopProject
  }

  private[bsp] def isScalaCliOrMill(workspace: Path): Boolean =
    BspSetupProvider.canImport(workspace, MillSetup) || BspSetupProvider.canImport(workspace, ScalaCliSetup)
}
