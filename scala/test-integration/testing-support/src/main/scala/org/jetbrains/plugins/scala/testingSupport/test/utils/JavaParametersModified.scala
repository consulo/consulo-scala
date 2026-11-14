package org.jetbrains.plugins.scala.testingSupport.test.utils

import com.intellij.java.language.vfs.jrt.JrtFileSystem
import consulo.content.base.BinariesOrderRootType
import consulo.content.bundle.Sdk
import consulo.execution.CantRunException
import consulo.java.execution.configurations.OwnJavaParameters
import consulo.java.language.fileTypes.JModFileType
import consulo.module.content.ProjectRootManager
import consulo.module.content.layer.orderEntry.{ModuleExtensionWithSdkOrderEntry, OrderEntry}
import consulo.module.content.layer.{OrderEnumerator, OrderRootsEnumerator}
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.project.{ModuleExt, ProjectExt}

import scala.jdk.CollectionConverters._

/**
 * (Hacky) Helper class needed to skip "build modules" classpath during tests run.
 * Example of "build module" is module created for sbt `project` folders (see any sbt project structure)
 * Such modules can contain Scala runtime libraries with version different from project version.
 * E.g. if you use sbt 1.3.x and scala 2.13.3, "build module" will use scala 2.12.x anyway
 */
private[test] class JavaParametersModified extends OwnJavaParameters {

  import OwnJavaParameters._

  /**
   * copied from [[consulo.java.execution.configurations.OwnJavaParameters.configureByProject]]
   * modified to include non-build modules classpath only
   */
  override def configureByProject(project: Project, classPathType: Int, jdk: Sdk): Unit = {
    if ((classPathType & JDK_ONLY) != 0) {
      if (jdk == null)
        throw CantRunException.noJdkConfigured
      setJdk(jdk)
    }

    if ((classPathType & CLASSES_ONLY) == 0)
      return

    setDefaultCharset(project)

    val nonBuildModules = project.modules.filterNot(_.isBuildModule)
    val nonBuildModulesRoots = ProjectRootManager.getInstance(project).orderEntries(nonBuildModules.asJavaCollection)
    configureEnumerator(nonBuildModulesRoots.runtimeOnly, classPathType, jdk).collectPaths(getClassPath)
  }

  private def configureEnumerator(enumerator0: OrderEnumerator, classPathType: Int, jdk: Sdk): OrderRootsEnumerator = {
    var enumerator = enumerator0
    if ((classPathType & INCLUDE_PROVIDED) == 0) enumerator = enumerator.runtimeOnly
    if ((classPathType & JDK_ONLY) == 0) enumerator = enumerator.withoutSdk
    if ((classPathType & TESTS_ONLY) == 0) enumerator = enumerator.productionOnly

    var rootsEnumerator = enumerator.classes

    if ((classPathType & JDK_ONLY) != 0)
      rootsEnumerator = rootsEnumerator.usingCustomRootProvider { (entry: OrderEntry) =>
        entry match {
          case _: ModuleExtensionWithSdkOrderEntry => jdkRoots(jdk)
          case _                                   => entry.getFiles(BinariesOrderRootType.ID)
        }
      }
    rootsEnumerator
  }

  private def jdkRoots(jdk: Sdk): Array[VirtualFile] = {
    val files = jdk.getRootProvider.getFiles(BinariesOrderRootType.ID)
    files.filterNot(f => JModFileType.isModuleRoot(f) || JrtFileSystem.isModuleRoot(f))
  }
}
