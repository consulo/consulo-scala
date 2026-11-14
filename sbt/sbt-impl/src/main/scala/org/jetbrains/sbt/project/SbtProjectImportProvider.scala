package org.jetbrains.sbt
package project

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.importing.{AbstractExternalModuleImportProvider, ExternalModuleImportContext}
import consulo.externalSystem.model.DataNode
import consulo.externalSystem.service.project.ProjectData
import consulo.externalSystem.service.project.manage.ProjectDataManager
import consulo.project.Project
import consulo.ui.image.Image
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.Inject
import org.jetbrains.annotations.Nullable
import org.jetbrains.sbt.api.RichVirtualFile
import org.jetbrains.sbt.api.language.SbtFileType

import java.io.File
import java.nio.file.{Files, Path}
import scala.jdk.StreamConverters.*
import scala.util.Using

// upstream: AbstractExternalProjectImportProvider + SbtProjectImportBuilder (AbstractExternalProjectImportBuilder);
// applyExtraSettings set the project JDK of the wizard from SbtProjectData.jdk - Consulo has no project JDK
@ExtensionImpl
final class SbtProjectImportProvider @Inject()(dataManager: ProjectDataManager)
  extends AbstractExternalModuleImportProvider(dataManager, SbtProjectSystem.Id) {

  override def getIcon: Image = Sbt.Icon

  override def canImport(fileOrDirectory: Path): Boolean =
    SbtProjectImportProvider.canImport(fileOrDirectory)

  override def getPathToBeImported(file: Path): String =
    SbtProjectImportProvider.projectRootPath(file)

  override protected def doPrepare(context: ExternalModuleImportContext): Unit = {}

  override protected def beforeCommit(dataNode: DataNode[ProjectData], project: Project): Unit = {}

  override protected def getExternalProjectConfigToUse(file: File): File = file

  override protected def applyExtraSettings(context: ExternalModuleImportContext): Unit = {}
}

object SbtProjectImportProvider {

  import SbtFileType.isMyFileType

  def canImport(@Nullable file: VirtualFile): Boolean = file match {
    case null => false
    case directory if directory.isDirectory =>
      directory.getName == Sbt.ProjectDirectory ||
        containsSbtProjectDirectory(directory) ||
        containsSbtBuildFile(directory)
    case _ => isMyFileType(file)
  }

  private def containsSbtProjectDirectory(directory: VirtualFile) =
    directory.findChild(Sbt.ProjectDirectory) match {
      case null => false
      case projectDirectory =>
        projectDirectory.containsFile(Sbt.PropertiesFile) ||
          containsSbtBuildFile(projectDirectory)
    }

  private def containsSbtBuildFile(directory: VirtualFile) =
    directory.getChildren.exists(isMyFileType)

  def projectRootPath(file: VirtualFile): String = {
    val root = if (file.isDirectory && file.getName != Sbt.ProjectDirectory) file
    else file.getParent

    root.getPath
  }

  def canImport(@Nullable file: Path): Boolean = file match {
    case null => false
    case directory if Files.isDirectory(directory) =>
      fileName(directory) == Sbt.ProjectDirectory ||
        containsSbtProjectDirectory(directory) ||
        containsSbtBuildFile(directory)
    case _ => isSbtFile(file)
  }

  private def containsSbtProjectDirectory(directory: Path) = {
    val projectDirectory = directory.resolve(Sbt.ProjectDirectory)
    Files.isDirectory(projectDirectory) &&
      (Files.isRegularFile(projectDirectory.resolve(Sbt.PropertiesFile)) || containsSbtBuildFile(projectDirectory))
  }

  private def containsSbtBuildFile(directory: Path) =
    Using.resource(Files.list(directory))(_.toScala(LazyList).exists(isSbtFile))

  private def isSbtFile(file: Path) =
    Files.isRegularFile(file) && fileName(file).endsWith(Sbt.Extension)

  private def fileName(file: Path) =
    Option(file.getFileName).fold("")(_.toString)

  def projectRootPath(file: Path): String = {
    val root = if (Files.isDirectory(file) && fileName(file) != Sbt.ProjectDirectory) file
    else file.getParent

    root.toString
  }
}
