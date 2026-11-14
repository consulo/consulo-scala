package org.jetbrains.bsp.data

import consulo.annotation.component.ExtensionImpl
import consulo.content.bundle.Sdk
import consulo.externalSystem.model.DataNode
import consulo.project.Project
import consulo.versionControlSystem.root.VcsRootDetector
import consulo.versionControlSystem.root.VcsRoot
import consulo.versionControlSystem.{ProjectLevelVcsManager, VcsDirectoryMapping}
import consulo.virtualFileSystem.LocalFileSystem
import consulo.virtualFileSystem.util.VirtualFileUtil
import org.jetbrains.plugins.scala.project.external.{ScalaAbstractProjectDataService, SdkReference, SdkUtils}

import java.nio.file.Path
import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@ExtensionImpl
class BspProjectDataService extends ScalaAbstractProjectDataService[BspProjectData, Project](BspProjectData.Key) {

  override def importData(
    toImport: util.Collection[DataNode[BspProjectData]],
    project: Project,
    synchronous: Boolean
  ): Unit = {
    toImport.forEach { node =>
      val BspProjectData(_, vcsRootsCandidates, _) = node.getData
      // upstream: configureJdk sets the project JDK and language level, Consulo has no project JDK -
      // modules get it in their Java extension (BspMetadataService)
      configureVcs(vcsRootsCandidates.map(_.toPath), project)
    }
  }

  override def removeData(toRemove: util.Collection[? <: Project], project: Project, synchronous: Boolean): Unit = ()

  private def configureVcs(vcsRootsCandidates: collection.Seq[Path], project: Project): Unit = {
    val vcsManager = ProjectLevelVcsManager.getInstance(project)
    val currentVcsRoots = vcsManager.getAllVcsRoots
    val currentMappings = vcsManager.getDirectoryMappings

    val detectedRoots = {
      val detector = project.getInstance(classOf[VcsRootDetector])
      val detected = mutable.Set[VcsRoot]()
      vcsRootsCandidates
        .iterator
        .map(LocalFileSystem.getInstance.findFileByNioFile)
        .filter(_ != null)
        .foreach { virtualFile =>
          val isUnderDetectedVcsRoot = VirtualFileUtil.isUnder(virtualFile, detected.map(_.getPath).asJava)
          if (!isUnderDetectedVcsRoot) {
            val roots = detector.detect(virtualFile).asScala
            detected ++= roots
          }
        }
      detected
    }

    val newMappings = detectedRoots
      .filterNot(currentVcsRoots.contains)
      .filter(_.getVcs != null)
      .map(root => new VcsDirectoryMapping(root.getPath.getPath, root.getVcs.getName))
    val allMappings = (currentMappings.asScala ++ newMappings).asJava

    vcsManager.setDirectoryMappings(allMappings)
    if (newMappings.nonEmpty) {
      BspVcsRootExtension.onVcsRootAdded(project)
    }
  }
}

object BspProjectDataService {

  /**
   * The JDK of the BSP project. upstream: it is set as the project JDK
   */
  def findJdk(project: Project, data: BspProjectData): Option[Sdk] = {
    val BspProjectData(jdk, _, _) = data
    jdk
      .flatMap(SdkUtils.findOrCreateSdk(_: SdkReference, project))
      .orElse(SdkUtils.mostRecentRegisteredJdk(project))
  }
}
