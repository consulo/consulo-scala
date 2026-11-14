package org.jetbrains.plugins.scala.server.compiler
import com.intellij.java.language.projectRoots.{JavaSdkType, JavaSdkVersion}
import consulo.content.bundle.Sdk
import consulo.java.language.bundle.JavaSdkTypeUtil
import consulo.java.language.module.extension.JavaModuleExtension
import consulo.language.util.ModuleUtilCore
import consulo.project.Project
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode

object CompileServerJdkManager {

  def compileServerJdk(project: Project): Option[JdkWithVersion] =
    for {
      sdk <- CompileServerLauncher.compileServerSdk(project).toOption
      version <- getJdkVersion(sdk)
    } yield (sdk, version)
  
  def recommendedJdk(project: Project): JdkWithVersion =
    getProjectJdk(project)
      .filter { case (_, version) => isCompatible(version) }
      .filter { case (_, version) => isRecommendedVersionForProject(project, version) }
      .getOrElse {
        // The project JDK cannot run the JPS code inside the Scala Compile Server (JDK version < 11).
        // Use the JDK which the JPS build system uses as a fallback. This is usually the bundled
        // JetBrains Runtime JDK.
        getBuildProcessRuntimeJdk(project)
      }

  /**
   * The Scala Compile Server code is compiled to Java 8 bytecode.
   */
  private[compiler] def isCompatible(version: JavaSdkVersion): Boolean = version.isAtLeast(JavaSdkVersion.JDK_1_8)

  private[compiler] def isRecommendedVersionForProject(project: Project, version: JavaSdkVersion): Boolean = {
    // Compiler based highlighting is enabled and we need at least Java 11 to be able to execute the
    // JPS code inside the Scala Compile Server.
    !ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(project) || version.isAtLeast(JavaSdkVersion.JDK_11)
  }

  /**
   * Returns the runtime JDK of the IDE.
   * The method isn't thread-safe, so the synchronized is used.
   * @see SCL-17710
   */
  // upstream: BuildManager.getBuildProcessRuntimeSdk(project) - the JPS build process JDK (the project JDK or the bundled JDK)
  private[compiler] def getBuildProcessRuntimeJdk(project: Project): JdkWithVersion = synchronized {
    if (ideRuntimeJdk == null) {
      val sdk = JavaSdkType.getDefaultJavaSdkType.createJdk("IDE JDK", System.getProperty("java.home"))
      ideRuntimeJdk = (sdk, JavaSdkVersion.fromVersionString(System.getProperty("java.version")))
    }
    ideRuntimeJdk
  }

  private var ideRuntimeJdk: JdkWithVersion = scala.compiletime.uninitialized

  // upstream: the project JDK (ProjectRootManager.getProjectSdk), Consulo modules have own JDKs - the most recent JDK of
  // the Scala modules is used
  private def getProjectJdk(project: Project): Option[JdkWithVersion] = {
    val jdks = inReadAction {
      if (project.isDisposed) Seq.empty
      else project.modulesWithScala.flatMap(module => Option(ModuleUtilCore.getSdk(module, classOf[JavaModuleExtension[?]])))
    }
    jdks.distinct
      .flatMap(sdk => getJdkVersion(sdk).map((sdk, _)))
      .maxByOption { case (_, version) => version.ordinal() }
  }

  private def getJdkVersion(sdk: Sdk): Option[JavaSdkVersion] =
    sdk.getSdkType match {
      case _: JavaSdkType =>
        Option(JavaSdkTypeUtil.getVersion(sdk))
      case _ => None
    }

  private type JdkWithVersion = (Sdk, JavaSdkVersion)
}
