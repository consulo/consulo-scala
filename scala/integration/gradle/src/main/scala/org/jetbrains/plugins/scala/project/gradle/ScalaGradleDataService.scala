package org.jetbrains.plugins.scala
package project.gradle

import consulo.annotation.component.ExtensionImpl
import consulo.content.library.Library
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.util.{DisposeAwareProjectChange, ExternalSystemApiUtil}
import consulo.gradle.GradleConstants
import consulo.module.Module
import consulo.module.content.ModuleRootManager
import consulo.module.content.layer.ModifiableRootModel
import consulo.module.extension.MutableModuleExtension
import consulo.project.Project
import consulo.project.content.library.ProjectLibraryTable
import org.jetbrains.plugins.scala.api.NlsString
import org.jetbrains.plugins.scala.project.*
import org.jetbrains.plugins.scala.project.external.{ScalaAbstractProjectDataService, ScalaSdkUtils}
import org.jetbrains.plugins.scala.util.ScalaNotificationGroups

import java.util
import scala.jdk.CollectionConverters.*

@ExtensionImpl
class ScalaGradleDataService extends ScalaAbstractProjectDataService[ScalaModelData, Library](ScalaModelData.Key) {

  private val GradleExternalSystemReadableName = GradleConstants.SYSTEM_ID.getReadableName.get

  override def importData(
    toImport: util.Collection[DataNode[ScalaModelData]],
    project: Project,
    synchronous: Boolean
  ): Unit = {
    // upstream: ScalaSdkUtils.revertScalaSdkFromLibraries - migration of projects imported by older IntelliJ Scala plugin versions

    // upstream: the Scala SDK is configured for all Gradle source set modules, Consulo Gradle imports one module per Gradle project
    val modules = toImport.asScala.toSeq.flatMap { scalaNode =>
      Option(scalaNode.getData(ProjectKeys.MODULE))
        .flatMap(project.findIdeModuleOpt)
        .map(scalaNode.getData -> _)
    }
    if (modules.isEmpty) return

    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
      override def execute(): Unit =
        modules.foreach { case (scalaData, module) => configureModule(scalaData, module)(using project) }
    })
  }

  private def configureModule(scalaData: ScalaModelData, module: Module)(implicit project: Project): Unit = {
    module.configureScalaCompilerSettingsFrom(GradleExternalSystemReadableName, scalaData.compilerOptions.asScala.toSeq, project)

    Option(scalaData.scalaVersion) match {
      case Some(version) =>
        configureScalaSdk(module, version, scalaData)
      case None =>
        showWarning(NlsString(ScalaGradleBundle.message("gradle.dataService.scalaVersionCantBeDetected", module.getName)))
    }
  }

  private def configureScalaSdk(module: Module, compilerVersion: String, scalaData: ScalaModelData)(implicit project: Project): Unit = {
    val rootModel = ModuleRootManager.getInstance(module).getModifiableModel
    val projectLibrariesModel = ProjectLibraryTable.getInstance(project).getModifiableModel
    try {
      ScalaSdkUtils.configureScalaSdk(
        module,
        compilerVersion,
        // Gradle's ScalaModel#getScalaClasspath only exposes java.io.File; there is no nio.Path-based alternative.
        //noinspection SSBasedInspection
        scalaData.scalaClasspath.asScala.toSeq.map(_.toPath),
        scaladocExtraClasspath = Nil,
        Option(scalaData.compilerBridgeBinaryJar).map(_.toPath),
        ReplClasspath.fromPaths(scalaData.replClasspath.asScala.toSeq.map(_.toPath)),
        sdkPrefix = GradleExternalSystemReadableName,
        projectLibrariesModel,
        rootModel
      )
      enableScala(rootModel)
    } finally {
      projectLibrariesModel.commit()
      rootModel.commit()
    }
  }

  // Consulo: Scala is enabled in a module by the Scala module extension (which requires the Java extension)
  private def enableScala(rootModel: ModifiableRootModel): Unit =
    Seq("java", "scala").foreach { id =>
      val extension: MutableModuleExtension[?] = rootModel.getExtensionWithoutCheck(id)
      if (extension != null && !extension.isEnabled)
        extension.setEnabled(true)
    }

  override def removeData(toRemove: util.Collection[? <: Library], project: Project, synchronous: Boolean): Unit = ()

  private val Title: NlsString = NlsString(ScalaGradleBundle.message("gradle.sync"))
  // upstream: the "Gradle" notification group of the IntelliJ Gradle plugin
  private val BalloonGroup = ScalaNotificationGroups.scalaGeneral
  private val SystemId = GradleConstants.SYSTEM_ID

  private def showWarning(message: NlsString)(implicit project: Project): Unit =
    super.showWarning(Title, message, BalloonGroup, SystemId)
}
