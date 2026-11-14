package org.jetbrains.sbt.project.data.service

import com.intellij.java.language.LanguageLevel
import consulo.content.bundle.Sdk
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.util.{DisposeAwareProjectChange, ExternalSystemApiUtil}
import consulo.java.language.module.extension.JavaMutableModuleExtension
import consulo.module.Module
import consulo.module.content.ModuleRootManager
import consulo.module.content.layer.ModifiableRootModel
import consulo.project.Project
import org.jetbrains.plugins.scala.project.extension.ScalaMutableModuleExtension
import org.jetbrains.sbt.project.data.SbtProjectData

import scala.collection.mutable

/**
 * Consulo enables languages in a module with module extensions: imported sbt modules get the Java extension
 * (JDK, language level, javac options) and the Scala extension (which requires the Java extension).
 */
private[service] object SbtModuleExtensions {

  final val JavaExtensionId = "java"
  final val ScalaExtensionId = "scala"

  def executeProjectChangeAction(project: Project, synchronous: Boolean)(action: => Unit): Unit =
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
      override def execute(): Unit = action
    })

  def modifyModel(module: Module)(action: ModifiableRootModel => Unit): Unit = {
    val model = ModuleRootManager.getInstance(module).getModifiableModel
    try action(model)
    finally model.commit()
  }

  def enableJava(model: ModifiableRootModel): JavaMutableModuleExtension[?] = {
    val extension: JavaMutableModuleExtension[?] = model.getExtensionWithoutCheck(JavaExtensionId)
    if (!extension.isEnabled) {
      extension.setEnabled(true)
      model.addModuleExtensionSdkEntry(extension)
    }
    extension
  }

  def configureJava(model: ModifiableRootModel, jdk: Option[Sdk], languageLevel: Option[LanguageLevel]): JavaMutableModuleExtension[?] = {
    val extension = enableJava(model)
    jdk.foreach(extension.getInheritableSdk.set(null, _))
    languageLevel.foreach(extension.getInheritableLanguageLevel.set(null, _))
    extension
  }

  def enableScala(model: ModifiableRootModel): Unit = {
    enableJava(model)
    val extension: ScalaMutableModuleExtension = model.getExtensionWithoutCheck(ScalaExtensionId)
    if (!extension.isEnabled)
      extension.setEnabled(true)
  }

  def javaSdk(model: ModifiableRootModel): Option[Sdk] =
    Option(model.getExtensionWithoutCheck[JavaMutableModuleExtension[?]](JavaExtensionId)).flatMap(e => Option(e.getSdk))

  /**
   * upstream: modules inherit the project JDK, Consulo has no project JDK - the JDK chosen for the sbt project
   * (see [[SbtProjectDataService]]) is set to the Java extension of the modules.
   */
  final class ProjectJdkCache(project: Project) {
    private val cache = mutable.Map.empty[DataNode[?], Option[Sdk]]

    def jdkFor(node: DataNode[?]): Option[Sdk] =
      Option(ExternalSystemApiUtil.findParent(node, ProjectKeys.PROJECT)).flatMap { projectNode =>
        cache.getOrElseUpdate(projectNode, {
          val sbtProjectData = Option(ExternalSystemApiUtil.find(projectNode, SbtProjectData.Key)).flatMap(n => Option(n.getData))
          sbtProjectData.flatMap(SbtProjectDataService.findJdk(project, _))
        })
      }
  }
}
