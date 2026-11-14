package org.jetbrains.plugins.scala.server.compiler
import consulo.platform.Platform
import org.jetbrains.plugins.scala.compiler.EelPathTranslator

import java.nio.file.Path

object EelCompilerUtils {
  /**
   * Given a [[java.nio.file.Path]] instance, returns a local path string which can be used inside the target machine.
   * For example, it returns a UNIX filesystem path for a given path of a remote Linux platform.
   *
   * @note The code is duplicated in `org.jetbrains.sbt.eelPathExtensions`. Should be deduplicated in the future after
   *       we fully migrate to Scala 3.
   */
  def asTargetLocalPathString(path: Path, platform: Platform): String =
    if (isLocal(platform)) path.toString
    else EelPathTranslator.translate(path)

  /**
   * @return true if the platform is the machine where the IDE is running
   */
  def isLocal(platform: Platform): Boolean =
    platform == Platform.current()
}
