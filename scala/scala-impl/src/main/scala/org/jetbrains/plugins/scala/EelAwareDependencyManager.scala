package org.jetbrains.plugins.scala

import consulo.application.platform.PlatformTransfer
import consulo.platform.Platform
import org.jetbrains.plugins.scala.DependencyManagerBase.DependencyDescription

import java.nio.file.Path

// upstream: @ApiStatus.Internal
private[scala] class EelAwareDependencyManager extends DependencyManagerBase {
  /**
   * Resolves dependencies locally and transfers them to the target platform (the paths are returned as is for
   * the local platform). Blocking, must not be called on EDT.
   */
  // upstream: @ApiStatus.Internal
  def resolveSafeAndTransferToRemoteEel(platform: Platform, dependencies: DependencyDescription*): Seq[Path] = {
    val resolvedPaths = resolveSafe(dependencies*).toOption.getOrElse(Seq.empty).map(_.file)
    if (platform == Platform.current()) resolvedPaths
    else {
      val transfer = PlatformTransfer.getInstance()
      resolvedPaths.map(transfer.transfer(_, platform))
    }
  }
}
