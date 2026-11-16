package org.jetbrains.bsp.protocol.session

import bloop.rifle.{BloopRifleConfig, BloopRifleLogger, BloopThreads}
import consulo.container.boot.ContainerPathManager
import consulo.application.progress.ProgressIndicator
import consulo.content.bundle.Sdk
import org.jetbrains.bsp.protocol.session.BspServerConnector.BspCapabilities
import org.jetbrains.bsp.protocol.session.BspSession.Builder
import org.jetbrains.bsp.{BspBundle, BspError}
import org.jetbrains.plugins.scala.build.BuildReporter

import java.nio.file.{Files, Path}
import scala.concurrent.duration.*
import scala.util.Try

/**
 * Handles the local bloop launcher connection.
 */
private[protocol] class BloopLocalLauncherConnector(
  base: Path,
  compilerOutput: Path,
  capabilities: BspCapabilities,
  jdk: Sdk
) extends BloopLauncherConnectorBase(compilerOutput, capabilities, jdk) {

  override def connect(using reporter: BuildReporter, indicator: Option[ProgressIndicator]): Either[BspError, Builder] = {
    // upstream: PathManager.getCommonDataPath (shared by all JetBrains IDEs)
    val bloopDataStore = ContainerPathManager.get().getSystemDir.resolve("bloop")

    val details = BloopRifleConfig.default(
      BloopRifleConfig.Address.DomainSocket(bloopDataStore),
      bloopClasspath,
      workingDir = base
    ).copy(javaPath = java, retainedBloopVersion = retainedBloopVersion)

    reporter.log(BspBundle.message("bsp.protocol.starting.bloop"))
    val detailsStringRepresentation =
      s"""BloopRifleConfig:
         |  domainSocketPath = $bloopDataStore
         |  workingDir = $base
         |  javaPath = $java
         |  retainedBloopVersion = $retainedBloopVersion
         |""".stripMargin
    reporter.log(BspBundle.message("bsp.protocol.rifle.details", detailsStringRepresentation))

    val threads = BloopThreads.create()
    val (connection, socket, _) = bloop.rifle.BloopServer.bsp(details, base, threads, BloopRifleLogger.nop, 10.seconds, 30.seconds)

    val pid = Try(Files.readString(bloopDataStore.resolve("pid"))).toOption.flatMap(_.toIntOption).getOrElse(-1)

    Right(createBuilder(socket, connection, pid, base, Some(threads)))
  }
}
