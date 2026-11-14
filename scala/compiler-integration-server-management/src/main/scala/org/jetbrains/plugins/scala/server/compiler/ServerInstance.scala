package org.jetbrains.plugins.scala.server.compiler
import consulo.platform.ForwardedPort
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType
import org.jetbrains.plugins.scala.server.CompileServerPort

import java.nio.file.Path

private final class ServerInstance(
  val watcher: ProcessWatcher,
  val createdAtStackTrace: Throwable,
  val compileServerSystemDir: Path,
  val port: CompileServerPort,
  val forwardedPort: Option[ForwardedPort],
  val workingDir: Option[Path],
  val jdk: JDK,
  val jvmParameters: Set[String],
  val incrementalCompiler: IncrementalityType
) {

  private var _stopped: Boolean = false

  def running: Boolean = !_stopped && watcher.running

  def stopped: Boolean = _stopped

  def pid: String = watcher.pid.fold("N/A")(_.toString)

  def destroyAndWait(): Boolean = {
    _stopped = true
    val result = watcher.destroyAndWait()
    closeForwardedPort()
    result
  }

  def destroyAndWaitFor(timeoutMs: Long): Boolean = {
    _stopped = true
    val result = watcher.destroyAndWaitFor(timeoutMs)
    closeForwardedPort()
    result
  }

  def closeForwardedPort(): Unit =
    forwardedPort.foreach(_.close())

  def summary: String = {
    s"pid: $pid" +
      s", port: $port" +
      s", jdk: $jdk" +
      s", jvmParameters: ${jvmParameters.mkString(",")}" +
      s", incrementalCompiler: $incrementalCompiler" +
      s", stopped: ${_stopped}" +
      s", running: $running"
  }
}
