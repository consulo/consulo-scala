package org.jetbrains.plugins.scala.project.external

import consulo.project.Project
import com.intellij.openapi.projectRoots.{JavaSdk, ProjectJdkTable}
import consulo.content.bundle.Sdk
import consulo.util.io.FileUtil
import consulo.platform.Platform
import com.intellij.java.language.LanguageLevel
import org.apache.commons.codec.digest.DigestUtils
import org.jetbrains.plugins.scala.extensions.{inReadAction, inWriteAction}

import java.nio.file.Path
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

object SdkUtils {

  @deprecated(message = "Use findProjectSdk(SdkReference, Platform)", since = "2026.2")
  @Deprecated(since = "2026.2", forRemoval = true)
  // upstream: @ApiStatus.ScheduledForRemoval(inVersion = "2026.3")
  def findProjectSdk(sdkRef: SdkReference): Option[Sdk] =
    findProjectSdk(sdkRef, Platform.current())

  def findProjectSdk(sdkRef: SdkReference, project: Project): Option[Sdk] =
    findProjectSdk(sdkRef, Platform.current())

  def findProjectSdk(sdkRef: SdkReference, platform: Platform): Option[Sdk] =
    sdkRef match {
      case JdkByVersion(version) => findMostRecentJdkConfiguredInIde(sdk => Option(sdk.getVersionString).exists(_.contains(version)), platform)
      case JdkByName(version)    => findMostRecentJdkConfiguredInIde(_.getName == version, platform).orElse(findMostRecentJdkConfiguredInIde(_.getName.contains(version), platform))
      case JdkByHome(homeFile)   => findMostRecentJdkConfiguredInIde(
        sdk => {
          // Based on the commit history, this sophisticated comparison was added to cover some
          // edge case (although not much detail is available), so let's keep it as it is for safety.
          val canonicalHomePath = Try(homeFile.toRealPath()).getOrElse(homeFile.toAbsolutePath.normalize()).toString
          FileUtil.comparePaths(canonicalHomePath, sdk.getHomePath) == 0 ||
            FileUtil.pathsEqual(homeFile.toAbsolutePath.toString, sdk.getHomePath)
        },
        platform
      )
      case _                     => None
    }

  @deprecated(message = "Use findMostRecentJdkConfiguredInIde(Sdk => Boolean, Platform)", since = "2026.2")
  @Deprecated(since = "2026.2", forRemoval = true)
  // upstream: @ApiStatus.ScheduledForRemoval(inVersion = "2026.3")
  def findMostRecentJdkConfiguredInIde(condition: Sdk => Boolean): Option[Sdk] =
    findMostRecentJdkConfiguredInIde(condition, Platform.current())

  def findMostRecentJdkConfiguredInIde(condition: Sdk => Boolean, platform: Platform): Option[Sdk] = {
    import scala.math.Ordering.comparatorToOrdering
    val sdkType = JavaSdk.getInstance()

    inReadAction {
      val jdks = ProjectJdkTable.getInstance()
        .getSdksOfType(JavaSdk.getInstance())
        .asScala
        .filter(sdk => condition(sdk))

      if (jdks.isEmpty) None
      else Option(jdks.max(using comparatorToOrdering(using sdkType.versionComparator())))
    }
  }

  @deprecated(message = "Use mostRecentRegisteredJdk(Platform)", since = "2026.2")
  @Deprecated(since = "2026.2", forRemoval = true)
  // upstream: @ApiStatus.ScheduledForRemoval(inVersion = "2026.3")
  def mostRecentRegisteredJdk: Option[Sdk] =
    findMostRecentJdkConfiguredInIde(_ => true)

  def mostRecentRegisteredJdk(platform: Platform): Option[Sdk] =
    findMostRecentJdkConfiguredInIde(_ => true, platform)

  def mostRecentRegisteredJdk(project: Project): Option[Sdk] =
    findMostRecentJdkConfiguredInIde(_ => true, Platform.current())

  def defaultJavaLanguageLevelIn(jdk: Sdk): Option[LanguageLevel] =
    Option(LanguageLevel.parse(jdk.getVersionString))

  private def resolveName(home: Path): String = {
    val fileName = home.getFileName
    val suffix = if (fileName.toString == "jre") home.getParent.getFileName else fileName
    val baseName = s"BSP_$suffix"
    val names = ProjectJdkTable.getInstance.getAllJdks.map(_.getName)
    if (names.contains(baseName)) {
      baseName + DigestUtils.md5Hex(home.toString).take(10)
    } else {
      baseName
    }
  }

  @deprecated(message = "Use findOrCreateSdk(SdkReference, Project)", since = "2026.2")
  @Deprecated(since = "2026.2", forRemoval = true)
  // upstream: @ApiStatus.ScheduledForRemoval(inVersion = "2026.3")
  def findOrCreateSdk(sdkReference: SdkReference): Option[Sdk] = {
    def createFromHome = {
      Option(sdkReference).collect {
        case JdkByHome(home) =>
          val name = resolveName(home)
          val newJdk = JavaSdk.getInstance.createJdk(name, home.toString, home.getFileName.toString == "jre")
          ProjectJdkTable.getInstance.addJdk(newJdk)
          newJdk
      }
    }

    SdkUtils.findProjectSdk(sdkReference).orElse(createFromHome)
  }

  def findOrCreateSdk(sdkReference: SdkReference, project: Project): Option[Sdk] = {
    def createFromHome =
      Option(sdkReference).collect {
        case JdkByHome(home) =>
          val name = resolveName(home)
          val newJdk = JavaSdk.getInstance.createJdk(name, home.toString, home.getFileName.toString == "jre")
          ProjectJdkTable.getInstance(project).addJdk(newJdk)
          newJdk
      }

    SdkUtils.findProjectSdk(sdkReference, project).orElse(createFromHome)
  }

  def addJdkIfNotExists(sdk: Sdk): Unit = {
    val projectJdkTable = ProjectJdkTable.getInstance()
    if (projectJdkTable.findJdk(sdk.getName) == null)
      inWriteAction {
        projectJdkTable.addJdk(sdk)
      }
  }
}
