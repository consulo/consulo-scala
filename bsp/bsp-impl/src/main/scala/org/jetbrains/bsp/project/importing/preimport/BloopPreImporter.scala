package org.jetbrains.bsp.project.importing.preimport

import consulo.platform.Platform
import consulo.application.progress.ProgressIndicator
import consulo.content.bundle.Sdk
import consulo.util.io.FileUtil
import org.jetbrains.bsp.{BspBundle, BspJdkUtil}
import org.jetbrains.bsp.buildinfo.BuildInfo
import org.jetbrains.plugins.scala.build.{BuildMessages, BuildReporter}
import org.jetbrains.plugins.scala.extensions.invokeAndWait
import org.jetbrains.sbt.SbtUtil.{detectSbtVersion, sbtVersionParam}
import org.jetbrains.sbt.process.SbtRunner
import org.jetbrains.sbt.project.settings.SbtExecutionSettings
import org.jetbrains.sbt.project.SbtExternalSystemManager
import org.jetbrains.sbt.{Sbt, SbtUtil, normalizedLocalPath}
import org.jetbrains.sbt.api.SbtVersion

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Try

class BloopPreImporter(runSbt: (SbtRunner, ProgressIndicator) => Try[BuildMessages]) extends PreImporter {
  private val runner = new SbtRunner()
  override def cancel(): Unit = runner.cancel()
  def run(indicator: ProgressIndicator): Try[BuildMessages] = runSbt(runner, indicator)
}
object BloopPreImporter {
  //noinspection ApiStatus
  def apply(baseDir: Path, jdk: Sdk)(using reporter: BuildReporter, platform: Platform): BloopPreImporter = {
    // upstream: ProjectJdkTable.preconfigure() - not in Consulo
    val jdkExe = BspJdkUtil.vmExecutablePath(jdk)
    val jdkHome = Option(jdk.getHomePath).map(Path.of(_))
    val sbtLauncher = SbtUtil.defaultLauncherPath

    val injectedPlugins = s"""addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "${BuildInfo.bloopVersion}")"""
    val pluginFile = SbtUtil.createTemporarySbtFile(injectedPlugins, platform, projectOpt = None)
    Runtime.getRuntime.addShutdownHook(Thread(() => FileUtil.delete(pluginFile): Unit))

    val injectedSettings = """bloopExportJarClassifiers in Global := Some(Set("sources"))"""
    // upstream: EelPathKotlinUtils.createTemporaryFile(prefix, suffix, baseDir, eelDescriptor)
    val settingsFile = platform.fs().createTempFile("idea-bloop", Sbt.Extension)
    Files.writeString(settingsFile, injectedSettings, StandardCharsets.UTF_8)

    val sbtLauncherArgs = List(
      "early(addPluginSbtFile=\"\"\"" + pluginFile.normalizedLocalPath + "\"\"\")"
    )
    val sbtCommands = "bloopInstall"

    val projectSbtVersion = detectSbtVersion(baseDir, SbtUtil.defaultLauncherPath)
    val sbtVersion = SbtVersion.upgradeSbtVersionToTheLatestCompatible(projectSbtVersion)
    val upgradeParam =
      if (sbtVersion > projectSbtVersion)
        List(sbtVersionParam(sbtVersion))
      else List.empty

    val vmArgs = SbtExternalSystemManager.getVmOptions(Seq.empty, jdkHome) ++ upgradeParam

    try {
      val runDump = (runner: SbtRunner, indicator: ProgressIndicator) => runner.runSbt(
        indicator = indicator,
        directory = baseDir,
        vmExecutable = jdkExe,
        vmOptions = vmArgs,
        environment0 = Map.empty,
        sbtLauncher = sbtLauncher,
        sbtOptions = SbtExecutionSettings.SbtOptions.empty,
        sbtLauncherArgs = sbtLauncherArgs,
        sbtCommands = sbtCommands,
        reportMessage = BspBundle.message("bsp.resolver.creating.bloop.configuration.from.sbt"),
        passParentEnvironment = true
      )
      new BloopPreImporter(runDump)
    } finally {
      FileUtil.delete(settingsFile)
    }
  }
}
