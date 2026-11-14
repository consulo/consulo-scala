package org.jetbrains.bsp.data

import com.intellij.java.language.LanguageLevel
import consulo.content.bundle.Sdk
import consulo.externalSystem.model.{DataNode, ProjectKeys}
import consulo.externalSystem.util.{DisposeAwareProjectChange, ExternalSystemApiUtil}
import consulo.java.language.module.extension.JavaMutableModuleExtension
import consulo.module.Module
import consulo.module.content.ModuleRootManager
import consulo.module.content.layer.ModifiableRootModel
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.SdkUtils
import org.jetbrains.plugins.scala.project.extension.ScalaMutableModuleExtension

import scala.collection.mutable

/**
 * Consulo enables languages in a module with module extensions: imported BSP modules get the Java extension
 * (JDK, language level) and the Scala extension (which requires the Java extension).
 */
// upstream: BSP modules are Java/sbt module types and inherit the project JDK
private[bsp] object BspModuleExtensions {

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

  def javaSdk(module: Module): Option[Sdk] =
    Option(ModuleRootManager.getInstance(module).getExtension[consulo.java.language.module.extension.JavaModuleExtension[?]](JavaExtensionId))
      .flatMap(e => Option(e.getSdk))

  /**
   * upstream: modules inherit the project JDK, Consulo has no project JDK - the JDK of the BSP project
   * (see [[BspProjectDataService]]) is set to the Java extension of the modules which don't define their own JDK.
   */
  final class ProjectJdkCache(project: Project) {
    private val cache = mutable.Map.empty[DataNode[?], Option[Sdk]]

    def jdkFor(node: DataNode[?]): Option[Sdk] =
      Option(ExternalSystemApiUtil.findParent(node, ProjectKeys.PROJECT)).flatMap { projectNode =>
        cache.getOrElseUpdate(projectNode, {
          val bspProjectData = Option(ExternalSystemApiUtil.find(projectNode, BspProjectData.Key)).flatMap(n => Option(n.getData))
          bspProjectData.flatMap(BspProjectDataService.findJdk(project, _))
        })
      }
  }
}
