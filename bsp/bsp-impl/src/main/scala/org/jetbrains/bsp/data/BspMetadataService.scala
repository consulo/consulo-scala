package org.jetbrains.bsp.data

import com.intellij.java.language.LanguageLevel
import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.DataNode
import consulo.module.Module
import consulo.project.Project
import org.jetbrains.plugins.scala.project.external.{JdkByHome, JdkByVersion, ScalaAbstractProjectDataService, SdkUtils}
import org.jetbrains.sbt.asPath

import java.util
import scala.jdk.CollectionConverters.*

@ExtensionImpl
class BspMetadataService extends ScalaAbstractProjectDataService[BspMetadata, Module](BspMetadata.Key) {

  override def importData(
    toImport: util.Collection[DataNode[BspMetadata]],
    project: Project,
    synchronous: Boolean
  ): Unit = BspModuleExtensions.executeProjectChangeAction(project, synchronous) {
    val projectJdks = new BspModuleExtensions.ProjectJdkCache(project)
    toImport.asScala.foreach { node =>
      doImport(node, projectJdks)(using project)
    }
  }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()

  private def doImport(node: DataNode[BspMetadata], projectJdks: BspModuleExtensions.ProjectJdkCache)
                      (implicit project: Project): Unit = {
    project.getIdeModuleByNode(node).foreach { module =>
      val data = node.getData
      val jdkByHome = Option(data.javaHome).map(u => JdkByHome(u.uri.asPath(using project.getPlatform)))
      val jdkByVersion = Option(data.javaVersion).map(JdkByVersion(_))
      // upstream: model.inheritSdk() - the module inherits the project JDK unless the target defines its own
      val moduleJdk = jdkByHome
        .orElse(jdkByVersion)
        .flatMap(SdkUtils.findOrCreateSdk(_, project))
        .orElse(projectJdks.jdkFor(node))
        .orElse(BspModuleExtensions.javaSdk(module))

      val moduleJdkVersion = moduleJdk.map(_.getVersionString)

      val languageLevel = Option(data.languageLevel)
        .orElse(Option(data.javaVersion).map(LanguageLevel.parse)) // Fallback to javaVersion
        .orElse(moduleJdkVersion.map(LanguageLevel.parse)) // Fallback to the version of javaHome
        .flatMap(level => Option(level))

      // upstream: LanguageLevelModuleExtensionImpl - the language level is set in the Java module extension
      BspModuleExtensions.modifyModel(module) { model =>
        BspModuleExtensions.configureJava(model, moduleJdk, languageLevel)
      }
    }
  }
}
