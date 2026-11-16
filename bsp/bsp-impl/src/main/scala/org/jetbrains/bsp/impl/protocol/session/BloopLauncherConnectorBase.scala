package org.jetbrains.bsp.impl.protocol.session

import bloop.rifle.{BloopRifleConfig, BloopThreads, BloopVersion, BspConnection}
import consulo.content.bundle.Sdk
import org.jetbrains.bsp.BspJdkUtil
import org.jetbrains.bsp.buildinfo.BuildInfo
import org.jetbrains.bsp.impl.protocol.session.BspServerConnector.BspCapabilities
import org.jetbrains.bsp.impl.protocol.session.BspSession.Builder
import org.jetbrains.plugins.scala.DependencyManager
import org.jetbrains.plugins.scala.DependencyManagerBase.RichStr
import org.jetbrains.plugins.scala.extensions.PathExt

import java.net.Socket
import java.nio.file.Path
import scala.util.control.NonFatal

private[protocol] abstract class BloopLauncherConnectorBase(
  val compilerOutput: Path,
  val capabilities: BspCapabilities,
  val jdk: Sdk
) extends BspServerConnector {

  protected val bloopVersion: String = BuildInfo.bloopVersion

  protected val retainedBloopVersion =
    BloopRifleConfig.AtLeast(BloopVersion(bloopVersion))

  protected lazy val java: String =
    BspJdkUtil.vmExecutablePath(jdk).toString

  protected def bloopClasspath(version: String): Either[Throwable, Seq[Path]] = {
    val dependencies = Seq(
      ("ch.epfl.scala" % "bloop-frontend_2.12" % version).transitive()
    )
    Right(DependencyManager.resolve(dependencies *).map(_.file))
  }

  protected def createBuilder(socket: Socket, connection: BspConnection, pid: Long, base: Path, threads: Option[BloopThreads]): Builder = {
    def safeClose(close: => Unit): Unit =
      try {
        close
      } catch {
        case NonFatal(_) => ()
      }

    val cleanup = () => {
      safeClose(connection.stop())
      safeClose(socket.close())
      safeClose(threads.foreach(_.shutdown()))
    }

    val rootUri = base.toCanonicalPath.toUri
    val compilerOutputUri = compilerOutput.toCanonicalPath.toUri
    val initializeBuildParams = BspServerConnector.createInitializeBuildParams(rootUri, compilerOutputUri, capabilities)

    BspSession.builder(pid, socket.getInputStream, _root_.java.io.InputStream.nullInputStream(), socket.getOutputStream, initializeBuildParams, cleanup)
  }
}
