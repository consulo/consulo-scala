package org.jetbrains.plugins.scala

import consulo.platform.Platform
import org.jetbrains.plugins.scala.DependencyManagerBase.DependencyDescription

import java.nio.file.Path

/**
 * Upstream transfers resolved artifacts into a remote (eel) environment. Consulo [[Platform]] is the local machine of
 * the IDE process, so resolved paths are returned as is.
 */
// upstream: @ApiStatus.Internal
private[scala] class EelAwareDependencyManager extends DependencyManagerBase {
  // upstream: @ApiStatus.Internal
  def resolveSafeAndTransferToRemoteEel(platform: Platform, dependencies: DependencyDescription*): Seq[Path] =
    resolveSafe(dependencies*).toOption.getOrElse(Seq.empty).map(_.file)
}
