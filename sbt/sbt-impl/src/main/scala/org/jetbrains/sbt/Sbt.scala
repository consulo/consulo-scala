package org.jetbrains.sbt

import consulo.ui.image.Image
import org.jetbrains.annotations.NonNls
import org.jetbrains.sbt.api.icons.Icons
import org.jetbrains.sbt.project.module.{SbtNestedModuleData, SbtSourceSetData}

// upstream: lives in sbt-api
object Sbt {
  @NonNls val Name = "sbt"
  @NonNls val Extension = ".sbt"
  @NonNls val BuildFile = "build.sbt"
  @NonNls val PropertiesFile = "build.properties"
  @NonNls val ProjectDirectory = "project"
  @NonNls val PluginsFile = "plugins.sbt"
  @NonNls val TargetDirectory = "target"
  @NonNls val ModulesDirectory = ".idea/modules"
  @NonNls val BuildModuleSuffix = "-build"
  @NonNls val BuildLibraryPrefix = "sbt-"
  @NonNls val UnmanagedLibraryName = "unmanaged-jars"
  @NonNls val UnmanagedSourcesAndDocsName = "unmanaged-sources-and-docs"
  @NonNls val DefinitionHolderClasses: Seq[String] = Seq("sbt.Plugin", "sbt.Build")

  // this should be in sync with sbt.BuildUtil.baseImports
  @NonNls val DefaultImplicitImports: Seq[String] = Seq("sbt._", "Process._", "Keys._", "dsl._")

  val SbtModuleChildKeyInstance: SbtUtil.SbtModuleChildKey = SbtUtil.SbtModuleChildKey(SbtNestedModuleData.Key, SbtSourceSetData.Key)

  def Icon: Image = Icons.SBT
  def FolderIcon: Image = Icons.SBT_FOLDER
}
