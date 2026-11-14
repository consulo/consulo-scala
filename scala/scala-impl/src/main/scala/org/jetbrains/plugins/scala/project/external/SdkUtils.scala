package org.jetbrains.plugins.scala.project.external

import consulo.project.Project
import com.intellij.java.language.projectRoots.JavaSdkType
import consulo.content.bundle.{Sdk, SdkTable}
import consulo.util.io.DigestUtil
import consulo.util.io.FileUtil
import consulo.platform.{JavaVersion, Platform}
import com.intellij.java.language.LanguageLevel
import org.jetbrains.plugins.scala.extensions.{inReadAction, inWriteAction}

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import scala.util.Try

object SdkUtils {

  @deprecated(message = "Use findProjectSdk(SdkReference, Platform)", since = "2026.2")
  @Deprecated(since = "2026.2", forRemoval = true)
  // upstream: @ApiStatus.ScheduledForRemoval(inVersion = "2026.3")
  def findProjectSdk(sdkRef: SdkReference): Option[Sdk] =
    findProjectSdk(sdkRef, Platform.current())

  def findProjectSdk(sdkRef: SdkReference, project: Project): Option[Sdk] =
    findProjectSdk(sdkRef, project.getPlatform)

  def findProjectSdk(sdkRef: SdkReference, platform: Platform): Option[Sdk] =
    sdkRef match {
      case JdkByVersion(version) => findMostRecentJdkConfiguredInIde(sdk => Option(sdk.getVersionString).exists(_.contains(version)), platform)
      case JdkByName(version)    => findMostRecentJdkConfiguredInIde(_.getName == version, platform).orElse(findMostRecentJdkConfiguredInIde(_.getName.contains(version), platform))
      case JdkByHome(homeFile)   => findMostRecentJdkConfiguredInIde(
        sdk =>
          if (platform == Platform.current()) {
            // Based on the commit history, this sophisticated comparison was added to cover some
            // edge case (although not much detail is available), so let's keep it as it is for safety.
            val canonicalHomePath = Try(homeFile.toRealPath()).getOrElse(homeFile.toAbsolutePath.normalize()).toString
            FileUtil.comparePaths(canonicalHomePath, sdk.getHomePath) == 0 ||
              FileUtil.pathsEqual(homeFile.toAbsolutePath.toString, sdk.getHomePath)
          } else {
            // remote SDK: compare NIO paths of the SDK's platform file system
            val sdkHome = sdk.getHomeNioPath
            sdkHome != null && sdkHome == platform.fs().getPath(homeFile.toString)
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
    inReadAction {
      // upstream: ProjectJdkTable.getSdksOfType(JavaSdk.getInstance()), max by JavaSdk.versionComparator
      val jdks = SdkTable.getInstance()
        .getAllSdks
        .toSeq
        .filter(sdk => sdk.getSdkType.isInstanceOf[JavaSdkType] && sdk.getPlatform.getId == platform.getId && condition(sdk))

      if (jdks.isEmpty) None
      else Option(jdks.maxBy(sdk => Option(sdk.getVersionString).flatMap(v => Option(JavaVersion.tryParse(v))))(using JavaVersionOrdering))
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
    findMostRecentJdkConfiguredInIde(_ => true, project.getPlatform)

  private object JavaVersionOrdering extends Ordering[Option[JavaVersion]] {
    override def compare(x: Option[JavaVersion], y: Option[JavaVersion]): Int = (x, y) match {
      case (Some(a), Some(b)) => a.compareTo(b)
      case (Some(_), None)    => 1
      case (None, Some(_))    => -1
      case (None, None)       => 0
    }
  }

  def defaultJavaLanguageLevelIn(jdk: Sdk): Option[LanguageLevel] =
    Option(LanguageLevel.parse(jdk.getVersionString))

  private def resolveName(home: Path): String = {
    val fileName = home.getFileName
    val suffix = if (fileName.toString == "jre") home.getParent.getFileName else fileName
    val baseName = s"BSP_$suffix"
    val names = SdkTable.getInstance.getAllSdks.map(_.getName)
    if (names.contains(baseName)) {
      baseName + md5Hex(home.toString).take(10)
    } else {
      baseName
    }
  }

  // upstream: org.apache.commons.codec.digest.DigestUtils.md5Hex
  private def md5Hex(text: String): String =
    DigestUtil.md5().digest(text.getBytes(StandardCharsets.UTF_8)).map(b => f"${b & 0xff}%02x").mkString

  // upstream: JavaSdk.createJdk(name, home, isJre)
  private def createJdk(name: String, home: Path): Sdk =
    JavaSdkType.getDefaultJavaSdkType.createJdk(name, home.toString)

  @deprecated(message = "Use findOrCreateSdk(SdkReference, Project)", since = "2026.2")
  @Deprecated(since = "2026.2", forRemoval = true)
  // upstream: @ApiStatus.ScheduledForRemoval(inVersion = "2026.3")
  def findOrCreateSdk(sdkReference: SdkReference): Option[Sdk] = {
    def createFromHome = {
      Option(sdkReference).collect {
        case JdkByHome(home) =>
          val name = resolveName(home)
          val newJdk = createJdk(name, home)
          SdkTable.getInstance.addSdk(newJdk)
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
          val newJdk = createJdk(name, home)
          SdkTable.getInstance.addSdk(newJdk)
          newJdk
      }

    SdkUtils.findProjectSdk(sdkReference, project).orElse(createFromHome)
  }

  def addJdkIfNotExists(sdk: Sdk): Unit = {
    val sdkTable = SdkTable.getInstance()
    if (sdkTable.findSdk(sdk.getName) == null)
      inWriteAction {
        sdkTable.addSdk(sdk)
      }
  }

  /** Removes `sdk` from the global SDK table if that exact instance is still registered. */
  def removeJdkIfRegistered(sdk: Sdk): Unit = {
    val sdkTable = SdkTable.getInstance()
    // A different SDK can reuse the same name. Cleanup must not remove that replacement.
    if (sdkTable.findSdk(sdk.getName) eq sdk) {
      inWriteAction(sdkTable.removeSdk(sdk))
    }
  }
}
