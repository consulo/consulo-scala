package org.jetbrains.sbt
package project
package data
package service

import consulo.annotation.component.ExtensionImpl
import consulo.externalSystem.model.DataNode
import consulo.externalSystem.util.{ExternalSystemConstants, Order}
import consulo.module.Module
import consulo.module.content.layer.ModifiableRootModel
import consulo.project.Project
import org.jetbrains.plugins.scala.project.*
import org.jetbrains.plugins.scala.project.external.*
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

import java.util
import scala.jdk.CollectionConverters.*

@ExtensionImpl
@Order(ExternalSystemConstants.UNORDERED)
final class SbtModuleExtDataService extends ScalaAbstractProjectDataService[SbtModuleExtData, Module](SbtModuleExtData.Key) {

  override def importData(
    toImport: util.Collection[DataNode[SbtModuleExtData]],
    project: Project,
    synchronous: Boolean
  ): Unit = SbtModuleExtensions.executeProjectChangeAction(project, synchronous) {
    val projectJdks = new SbtModuleExtensions.ProjectJdkCache(project)
    for {
      dataNode <- toImport.asScala
      module <- project.getIdeModuleByNode(dataNode)
      SbtModuleExtData(scalacOptions, sdk, javacOptions, kotlincOptions, packagePrefix, basePackage, compileOrder) = dataNode.getData
    } {
      module.configureScalaCompilerSettingsFrom("sbt", scalacOptions.asScala, project, compileOrder)
      SbtModuleExtensions.modifyModel(module) { model =>
        configureOrInheritSdk(model, Option(sdk), projectJdks.jdkFor(dataNode), project)
        importJavacOptions(model, javacOptions.asScala.toSeq)
      }
      // upstream: kotlinc options are imported with SbtKotlinCompilerOptionsImporter (kotlin plugin), not available in Consulo
      // upstream: packagePrefix is set to the source folders, Consulo source folders have no package prefixes
      ScalaProjectSettings.getInstance(project).setCustomBasePackage(module.getName, basePackage)
    }
  }

  override def removeData(toRemove: util.Collection[? <: Module], project: Project, synchronous: Boolean): Unit = ()

  // upstream: inherits the project JDK and sets the module JDK, Consulo has no project JDK - the JDK is set to the Java module extension
  private def configureOrInheritSdk(model: ModifiableRootModel, sdk: Option[SdkReference], projectJdk: => Option[consulo.content.bundle.Sdk], project: Project): Unit = {
    val moduleSdk = sdk.flatMap(SdkUtils.findProjectSdk(_, project))
    SbtModuleExtensions.configureJava(model, moduleSdk.orElse(projectJdk), languageLevel = None)
  }

  private def importJavacOptions(model: ModifiableRootModel, javacOptions: Seq[String]): Unit = {
    configureLanguageLevel(model, javacOptions)
    configureTargetBytecodeLevel(model, javacOptions)
    configureJavacOptions(model, javacOptions)
  }

  private def configureLanguageLevel(model: ModifiableRootModel, javacOptions: Seq[String]): Unit = {
    val moduleSdk = SbtModuleExtensions.javaSdk(model)
    val languageLevelFromJavac = JavacOptionsUtils.javaLanguageLevel(javacOptions)
    val languageLevel = languageLevelFromJavac.orElse(moduleSdk.flatMap(SdkUtils.defaultJavaLanguageLevelIn))
    languageLevel.foreach { level =>
      SbtModuleExtensions.enableJava(model).getInheritableLanguageLevel.set(null, level)
    }
  }

  private def configureTargetBytecodeLevel(model: ModifiableRootModel, javacOptions: Seq[String]): Unit = {
    val targetValueFromJavac = JavacOptionsUtils.effectiveTargetValue(javacOptions)
    SbtModuleExtensions.enableJava(model).setBytecodeVersion(targetValueFromJavac.orNull)
  }

  private def configureJavacOptions(model: ModifiableRootModel, javacOptions0: Seq[String]): Unit = {
    val javacOptions = JavacOptionsUtils.withoutExplicitlyHandledOptions(javacOptions0)

    val extension = SbtModuleExtensions.enableJava(model)

    // upstream: module options equal to the project javac options (JavacCompilerConfiguration) are treated as empty, because
    // IntelliJ falls back to the project options for a module without own options; the arguments of the Consulo Java module
    // extension have no such fallback (javac gets both), so they are compared as they are
    val moduleCurrentOptions = Option(extension.getCompilerArguments).map(_.asScala.toSeq).getOrElse(Nil)

    if (javacOptions != moduleCurrentOptions) {
      extension.setCompilerArguments(javacOptions.asJava)
    }
  }
}
