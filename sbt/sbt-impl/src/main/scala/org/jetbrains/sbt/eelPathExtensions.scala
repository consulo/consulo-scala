//noinspection ApiStatus,UnstableApiUsage
package org.jetbrains.sbt

import consulo.application.Application
import consulo.application.platform.PlatformTargetManager
import consulo.platform.Platform
import consulo.util.io.FileUtil
import org.jetbrains.plugins.scala.compiler.EelPathTranslator

import java.nio.file.{FileSystems, Path}

// upstream: EEL API (EelDescriptor) - Consulo Platform API is used instead
extension (path: Path) {
  /**
   * The platform (machine) the path belongs to.
   */
  def platform: Platform =
    if (path.getFileSystem == FileSystems.getDefault) Platform.current()
    else
      Option(Application.get().getInstance(classOf[PlatformTargetManager]).findTargetOf(path))
        .flatMap(target => Option(target.getPlatform))
        .getOrElse(Platform.current())

  def normalizedLocalPath: String =
    FileUtil.toSystemIndependentName(path.asLocalPath)

  /**
   * A machine-specific local path.
   */
  def asLocalPath: String =
    EelPathTranslator.translate(path)
}

/**
 * @return true if the platform is the machine where the IDE is running
 */
def isLocalPlatform(platform: Platform): Boolean =
  platform == Platform.current()
