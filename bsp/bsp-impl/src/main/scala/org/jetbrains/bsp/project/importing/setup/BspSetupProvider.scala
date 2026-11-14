package org.jetbrains.bsp.project.importing.setup

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.component.extension.ExtensionPointName
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.bsp.project.importing.bspConfigSteps.ConfigSetup

import java.nio.file.Path
import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * Extension point for providing BSP configuration setup for various build tools.
 * In practice used internally for Scala CLI and Mill.
 */
// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
trait BspSetupProvider {
  /** Returns the config setup type this provider handles (e.g., MillSetup, ScalaCliSetup). */
  def configSetup: ConfigSetup

  /** Checks if this provider can import the project in the given workspace. */
  def canImport(workspace: Path): Boolean

  def getBspConfigSetup(workspace: Path): BspConfigSetup

  /**
   * File names whose presence in the project root indicates the project belongs to this BSP setup provider.
   */
  def bspBuildFileNames(project: Project): Seq[String]
}

private[bsp] object BspSetupProvider {

  private val EP = ExtensionPointName.create(classOf[BspSetupProvider])

  /** Checks if a provider for the specific config setup can import the project in the given workspace. */
  def canImport(workspace: Path, configSetup: ConfigSetup): Boolean =
    getProvider(workspace, configSetup).nonEmpty

  /** Get the [[BspConfigSetup]] for the specific config setup type. */
  def getBspConfigSetup(workspace: Path, configSetup: ConfigSetup): Option[BspConfigSetup] =
    getProvider(workspace, configSetup).map(_.getBspConfigSetup(workspace))

  private def getProvider(workspace: Path, configSetup: ConfigSetup): Option[BspSetupProvider] =
    getImplementations.find(p => p.configSetup == configSetup && p.canImport(workspace))

  /**
   * Checks if the given file is recognized as a build file by any registered BSP setup provider.
   */
  def isBuildFile(file: VirtualFile, project: Project): Boolean =
    getImplementations.exists(_.bspBuildFileNames(project).contains(file.getName)) && !file.isDirectory

  private def getImplementations: Seq[BspSetupProvider] =
    EP.getExtensionList.iterator().asScala.toSeq
}
