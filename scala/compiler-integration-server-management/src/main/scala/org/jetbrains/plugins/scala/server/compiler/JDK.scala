package org.jetbrains.plugins.scala.server.compiler
import com.intellij.java.language.projectRoots.{JavaSdkType, JavaSdkVersion}
import consulo.content.bundle.Sdk
import consulo.java.language.bundle.JavaSdkTypeUtil
import org.jetbrains.plugins.scala.server.compiler.CompileServerLauncher.CompileServerProblem

import java.nio.file.Path

/**
 * @param executable the `java` executable, a path of the SDK platform file system
 */
final case class JDK(executable: Path, tools: Option[Path], name: String, version: Option[JavaSdkVersion])

object JDK {
  def fromSdk(sdk: Sdk): Either[CompileServerProblem, JDK] = sdk.getSdkType match {
    case jdkType: JavaSdkType if sdk.getHomeNioPath != null =>
      // upstream: JavaSdk.getVMExecutablePath(sdk)
      val executableName = if (sdk.getPlatform.os().isWindows) "java.exe" else "java"
      val home = sdk.getHomeNioPath
      val vmExecutable = home.resolve("bin").resolve(executableName)
      val tools = Option(jdkType.getToolsPath(sdk)).map(home.getFileSystem.getPath(_)) // TODO properly handle JDK 6 on Mac OS
      val version = Option(JavaSdkTypeUtil.getVersion(sdk))
      Right(JDK(vmExecutable, tools, sdk.getName, version))
    case unexpected =>
      Left(CompileServerProblem.Error(ServerManagementBundle.message("unexpected.sdk.type.for.sdk", unexpected, sdk)))
  }
}
