package org.jetbrains.sbt.project

import consulo.logging.Logger
import consulo.component.ProcessCanceledException
import consulo.application.progress.ProgressManager
import consulo.project.Project
import com.intellij.java.language.projectRoots.{JavaSdkType, JavaSdkVersion}
import consulo.content.bundle.{Sdk, SdkTable, SdkUtil}
import consulo.application.util.function.ThrowableComputable
import consulo.java.language.bundle.JavaSdkTypeUtil
import consulo.platform.{JavaVersion, Platform}
import consulo.ui.annotation.RequiredUIAccess
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.extensions.inWriteAction
import org.jetbrains.sbt.project.template.wizard.JdkSbtCompatibilityChecker
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.api.SbtVersion

import java.nio.file.Path
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.math.Ordered.orderingToOrdered

/**
 * This class contains helper utilities to find JDK with the most suitable version to run SBT.
 *
 * It's only necessary when Project SDK is not configured, for example, during initial project loading.
 *
 * Using "latest" java is not an option because if a user has some non-release JDK version installed, then the SBT import process can fail.
 * (For example, sbt 1.5.1 doesn't support JDK 18)
 *
 * @note In some cases it won't work.<br>
 *       For example, if the user uses an old SBT version which only supports JDK 8 but the user has JDK 17 installed.
 * @see https://docs.scala-lang.org/overviews/jdk-compatibility/overview.html
 */
object SbtProcessJdkGuesser {

  private val Log = Logger.getInstance(getClass)

  private val MINIMUM_JAVA_SDK_VERSION: JavaSdkVersion = JavaSdkVersion.JDK_1_8
  private val MAXIMUM_JAVA_SDK_VERSION: JavaSdkVersion = JavaSdkVersion.JDK_17

  private def jdkType: JavaSdkType = JavaSdkType.getDefaultJavaSdkType
  // upstream: JavaSdk.versionComparator
  private val versionOrdering: Ordering[Sdk] = Ordering.by(sdk => Option(JavaSdkTypeUtil.getVersion(sdk)).fold(-1)(_.ordinal()))

  /**
   * This is an alternative to `JavaAwareProjectJdkTableImpl.preconfigure()` which selects most recent JDK<br>
   * To be safe we don't need most recent JDK (see docs for [[findJdkWithSuitableVersion]])
   */
  @RequiredUIAccess
  def preconfigureJdkForSbt(project: Project, jdkTable: SdkTable, sbtVersion: SbtVersion): Unit = {
    try {
      val jdkOpt = ProgressManager.getInstance.runProcessWithProgressSynchronously(
        (() => createJdkWithSuitableVersion(sbtVersion, project.getPlatform)): ThrowableComputable[Option[Sdk], Exception],
        SbtBundle.message("sbt.import.detecting.jdk"),
        true,
        project
      )
      jdkOpt.foreach { jdk =>
        inWriteAction {
          jdkTable.addSdk(jdk)
        }
      }
    } catch {
      case _: ProcessCanceledException =>
      //ignore
    }
  }

  case class SdkCandidate(sdk: Option[Sdk], allSdkSorted: Seq[Sdk])

  def findJdkWithSuitableVersion(jdkTable: SdkTable, sbtVersion: SbtVersion): SdkCandidate = {
    val sdksAll = JavaSdkTypeUtil.getAllJavaSdks.asScala.toSeq
    val sdksAllSorted = sdksAll.sorted(using versionOrdering)
    val filteredBySbt = sdksAllSorted.filter { sdk =>
      isSbtJdkCompatible(JavaSdkTypeUtil.getVersion(sdk), sbtVersion)
    }

    val sdksMatchingVersion =
      if (filteredBySbt.nonEmpty) filteredBySbt
      else {
        sdksAllSorted.filter { sdk =>
          isInRange(JavaSdkTypeUtil.getVersion(sdk))
        }
      }

    if (Log.isTraceEnabled) {
      Log.trace(s"findMostSuitableJdkForSbt: all sdks:\n${sdksAllSorted.mkString("\n")}")
    }

    SdkCandidate(sdksMatchingVersion.headOption, sdksAllSorted)
  }

  /**
   * Tries to find all existing sdk home paths on the local machine
   *
   * @return suggested sdk home paths and corresponding versions
   */
  // upstream: @RequiresBackgroundThread
  def findAllExistingJavaPaths(jdkType: JavaSdkType, platform: Platform): Seq[(Path, JavaVersion)] = {
    // upstream: JavaHomeFinder.suggestHomePaths
    val javaPaths = Seq.newBuilder[Path]
    jdkType.collectHomePaths(platform, javaPaths += _)
    javaPaths.result()
      .distinct
      .filter(jdkType.isValidSdkHome(platform, _))
      .flatMap { path =>
        for
          versionString <- Option(jdkType.getVersionString(platform, path))
          javaVersion <- Option(JavaVersion.tryParse(versionString))
        yield path -> javaVersion
      }
  }

  private def isSbtJdkCompatible(@Nullable sdk: JavaSdkVersion, sbtVersion: SbtVersion): Boolean = {
    sdk != null && JdkSbtCompatibilityChecker.isSbtAndJdkVersionCompatible(sdk.getMaxLanguageLevel.toJavaVersion, sbtVersion, strict = true)
  }

  private def isInRange(@Nullable sdk: JavaSdkVersion): Boolean =
    sdk != null && MINIMUM_JAVA_SDK_VERSION <= sdk && sdk <= MAXIMUM_JAVA_SDK_VERSION


  /** Alternative for `JavaAwareProjectJdkTableImpl.guessJdk()` */
  // upstream: @RequiresBackgroundThread
  private def createJdkWithSuitableVersion(sbtVersion: SbtVersion, platform: Platform): Option[Sdk] = {
    val javaPaths0 = findAllExistingJavaPaths(jdkType, platform)

    val javaPaths = javaPaths0.flatMap { case (path, version) =>
      Option(JavaSdkVersion.fromJavaVersion(version)).map(JavaPathWithVersion(path, version, _))
    }

    val filteredBySbt = javaPaths.filter(javaPath => isSbtJdkCompatible(javaPath.sdkVersion, sbtVersion))

    val javaPathsMatchingVersion =
      if (filteredBySbt.nonEmpty) filteredBySbt
      else {
        javaPaths.filter(javaPath => isInRange(javaPath.sdkVersion))
      }


    val homePath = javaPathsMatchingVersion.headOption match {
      case Some(value) => value
      case None =>
        return None
    }
    val allJdks = SdkTable.getInstance().getAllSdks
    val suggestedName = SdkUtil.createUniqueSdkName(platform, jdkType, homePath.path, allJdks)

    ProgressManager.checkCanceled()

    Option(jdkType.createJdk(suggestedName, homePath.path.toString))
  }

  private case class JavaPathWithVersion(path: Path, version: JavaVersion, sdkVersion: JavaSdkVersion)
}
