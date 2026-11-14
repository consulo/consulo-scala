package org.jetbrains.sbt
package project.data
package service

import com.intellij.java.compiler.impl.javaCompiler.javac.JavacCompilerConfiguration
import consulo.annotation.component.ExtensionImpl
import consulo.content.bundle.Sdk
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.platform.JavaVersion
import consulo.project.Project
import consulo.project.content.library.ProjectLibraryTable
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType
import org.jetbrains.plugins.scala.project.*
import org.jetbrains.plugins.scala.project.external.*
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration
import org.jetbrains.sbt.api.SbtVersion
import org.jetbrains.sbt.project.SharedSourcesOwnersData
import org.jetbrains.sbt.project.settings.SbtProjectSettings
import org.jetbrains.sbt.project.template.wizard.JdkSbtCompatibilityChecker
import org.jetbrains.sbt.settings.SbtSettings

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala

@ExtensionImpl
class SbtProjectDataService extends ScalaAbstractProjectDataService[SbtProjectData, Project](SbtProjectData.Key) {

  override def importData(
    toImport: util.Collection[DataNode[SbtProjectData]],
    project: Project,
    synchronous: Boolean
  ): Unit = {
    val dataToImport = toImport.asScala
    // upstream: removes ContentRootDataService.CREATE_EMPTY_DIRECTORIES user data, Consulo doesn't create empty directories
    SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
      // note: this condition is required because sbt service might also be invoked for non-sbt projects
      if (dataToImport.nonEmpty) {
        revertScalaSdkFromLibraries(project)
      }
      dataToImport.foreach(node => doImport(project, node))
    }
  }

  override def removeData(toRemove: util.Collection[? <: Project], project: Project, synchronous: Boolean): Unit = ()

  // This was implemented because, without this method, if a user reloads a project that was previously imported
  // in a plugin version where existing Scala libraries were converted to Scala SDKs, it would result in having multiple Scala SDKs.
  // This is a partial duplication with org.jetbrains.plugins.scala.project.external.ScalaSdkUtils.revertScalaSdkFromLibraries.
  // However, since that method was introduced just a few days before the release, I’m not using it here to avoid breaking anything.
  // TODO This was introduced in 2023. By now, all users should have updated IDEA to a newer version,
  //  so it might be possible to remove this in 2025.x or 2026.x.
  private def revertScalaSdkFromLibraries(project: Project): Unit = {
    val libraries = ProjectLibraryTable.getInstance(project).getLibraries.filter(_.hasRuntimeLibrary)
    /* note: there is a possibility that in IDEA we will have projects with different subsystems (I think it is a very rare case
     but from a technical point of view it is possible). In such case we do not want to remove scala SDK kind from libraries that come from systems other than SBT.
     "sbt: scala-sdk" prefix is left because it indicates that this SDK comes from the new implementation of Scala SDK and shouldn't be removed.
     The only way I could find to check from which system the library comes from is consulo.externalSystem.util.ExternalSystemApiUtil.isExternalSystemLibrary method, but
     the logic there is also based on checking the prefix in the library name

    TODO checking if library name does not start with hardcoded prefixes ("Gradle:", "Maven:", "BSP:") is not an ideal implementation.
     It would be best to identify which system the library comes from by a different-more predictable value
    */
    libraries
      .filter { library => library.isScalaSdk && !Seq("sbt: scala-sdk", "Gradle:", "Maven:", "BSP:").exists(library.getName.startsWith)}
      .foreach { library =>
        val model = library.getModifiableModel
        model.setKind(null)
        model.commit()
    }
  }

  private def doImport(project: Project, node: DataNode[SbtProjectData]): Unit = {
    val data = node.getData
    SbtProjectSettings.forProject(project).foreach(_.converterVersion = SbtProjectSettings.ConverterVersion)
    // upstream: configureJdk sets the project JDK, Consulo has no project JDK - modules get it in their Java extension
    setDefaultJavacOptions(project)
    setSbtVersion(project, data)
    updateIncrementalityType(project, node)
    updateSeparateProdTestSources(project, data.prodTestSourcesSeparated)
  }

  // upstream: also resets the project language level and the project bytecode target, these are module extension settings in Consulo
  private def setDefaultJavacOptions(project: Project): Unit = {
    val javacOptions = JavacCompilerConfiguration.getInstance(project)
    javacOptions.ADDITIONAL_OPTIONS_STRING = ""
  }

  private def setSbtVersion(project: Project, data: SbtProjectData): Unit =
    Option(SbtSettings.getInstance(project).getLinkedProjectSettings(data.projectPath))
      .foreach(s => s.sbtVersion = data.sbtVersion)

  private def updateSeparateProdTestSources(project: Project, separateProdTestSources: Boolean): Unit =
    ScalaCompilerConfiguration.instanceIn(project).separateProdTestSources = separateProdTestSources

  // upstream: checks for modules of SharedSourcesModuleType, Consulo has no module types - shared sources modules carry SharedSourcesOwnersData
  private def updateIncrementalityType(project: Project, node: DataNode[SbtProjectData]): Unit = {
    val projectNode = Option(ExternalSystemApiUtil.findParent(node, ProjectKeys.PROJECT))
    if (projectNode.exists(hasSharedSourcesModules))
      ScalaCompilerConfiguration.instanceIn(project).incrementalityType = IncrementalityType.SBT
  }

  private def hasSharedSourcesModules(node: DataNode[?]): Boolean =
    node.getKey == SharedSourcesOwnersData.Key || node.getChildren.asScala.exists(hasSharedSourcesModules)
}

object SbtProjectDataService {

  /**
   * The JDK of the sbt project. upstream: it is set as the project JDK
   */
  def findJdk(project: Project, data: SbtProjectData): Option[Sdk] = {
    val platform = project.getPlatform

    val jdk1 = Option(data.jdk).flatMap(SdkUtils.findProjectSdk(_, platform))
    val jdk2 = jdk1.orElse {
      SdkUtils.findMostRecentJdkConfiguredInIde(
        sdk =>
          val sbtVersion = SbtVersion(data.sbtVersion)
          Option(JavaVersion.tryParse(sdk.getVersionString)).exists { sdkVersion =>
            JdkSbtCompatibilityChecker.isSbtAndJdkVersionCompatible(sdkVersion, sbtVersion, strict = true)
          },
        platform
      )
    }
    jdk2.orElse(SdkUtils.mostRecentRegisteredJdk(platform))
  }
}
