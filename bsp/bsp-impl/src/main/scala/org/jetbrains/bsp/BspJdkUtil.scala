package org.jetbrains.bsp

import com.intellij.java.language.projectRoots.JavaSdkType
import consulo.application.progress.ProgressManager
import consulo.application.util.function.ThrowableComputable
import consulo.content.bundle.{Sdk, SdkTable, SdkUtil}
import consulo.externalSystem.util.ExternalSystemApiUtil
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.util.ModuleUtilCore
import consulo.module.ModuleManager
import consulo.platform.{JavaVersion, Platform}
import consulo.project.Project
import org.jetbrains.plugins.scala.extensions.inWriteAction
import org.jetbrains.plugins.scala.project.external.SdkUtils
import org.jetbrains.sbt.project.SbtProcessJdkGuesser

import java.nio.file.Path

object BspJdkUtil {

  /**
   * Returns JDK assigned to a project or most recent registered JDK in IDEA. If the first two ways return nothing
   * then try to create and return SDK based on most recent JDK found on the machine.
   */
  @deprecated(message = "Use findOrCreateBestJdkForProject(Option[Project], Platform)", since = "2026.2")
  @Deprecated(since = "2026.2", forRemoval = true)
  // upstream: @ApiStatus.ScheduledForRemoval(inVersion = "2026.3")
  def findOrCreateBestJdkForProject(project: Option[Project]): Option[Sdk] =
    findOrCreateBestJdkForProject(project, project.map(_.getPlatform).getOrElse(Platform.current()))

  def findOrCreateBestJdkForProject(project: Project): Option[Sdk] =
    findOrCreateBestJdkForProject(Some(project), project.getPlatform)

  /**
   * Returns JDK assigned to a project or most recent registered JDK in IDEA. If the first two ways return nothing
   * then try to create and return SDK based on most recent JDK found on the machine.
   */
  def findOrCreateBestJdkForProject(project: Option[Project], platform: Platform): Option[Sdk] =
    project.flatMap(projectJdk)
      .orElse(SdkUtils.mostRecentRegisteredJdk(platform))
      .orElse(createSdkWithMostRecentFoundJDK(project, platform))

  // upstream: JavaSdk.getVMExecutablePath
  def vmExecutablePath(jdk: Sdk): Path = {
    val javaExe = if (Platform.current().os().isWindows) "java.exe" else "java"
    jdk.getSdkType match {
      case sdkType: JavaSdkType => Path.of(sdkType.getBinPath(jdk)).resolve(javaExe)
      case _ => Path.of(jdk.getHomePath).resolve("bin").resolve(javaExe)
    }
  }

  // upstream: ProjectRootManager.getProjectSdk - Consulo has no project JDK, the JDK of the first module having one
  private def projectJdk(project: Project): Option[Sdk] =
    ModuleManager.getInstance(project).getModules.iterator
      .flatMap(module => Option(ModuleUtilCore.getSdk(module, classOf[JavaModuleExtension[?]])))
      .nextOption()

  private def createSdkWithMostRecentFoundJDK(project: Option[Project], platform: Platform): Option[Sdk] = {
    val jdkType = JavaSdkType.getDefaultJavaSdkType

    val detectedJavaHomes: Seq[(Path, JavaVersion)] = ProgressManager.getInstance.runProcessWithProgressSynchronously(
      (() => SbtProcessJdkGuesser.findAllExistingJavaPaths(jdkType, platform)): ThrowableComputable[Seq[(Path, JavaVersion)], Exception],
      BspBundle.message("bsp.import.detecting.jdk"),
      true,
      project.orNull
    )

    val latestJavaHome: Option[Path] = detectedJavaHomes
      .maxByOption(_._2)
      .map(_._1)

    // upstream: SdkConfigurationUtil.createAndAddSDK
    latestJavaHome.map { home =>
      ExternalSystemApiUtil.executeOnEdt(() => {
        val name = SdkUtil.createUniqueSdkName(platform, jdkType, home, SdkTable.getInstance().getAllSdks)
        val sdk = jdkType.createJdk(name, home.toString)
        inWriteAction(SdkTable.getInstance().addSdk(sdk))
        sdk
      })
    }
  }
}
