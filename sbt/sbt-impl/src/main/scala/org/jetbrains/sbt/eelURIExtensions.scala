//noinspection ApiStatus,UnstableApiUsage
package org.jetbrains.sbt

import consulo.platform.Platform

import java.net.URI
import java.nio.file.Path

extension (uri: URI) {

  /**
   * Convert [[URI]] to [[Path]] on the given [[Platform]].
   *
   * For the local platform, returns a local path.
   * For remote platforms, returns a path of the platform file system.
   */
  def asPath(using platform: Platform): Path =
    if (isLocalPlatform(platform)) Path.of(uri)
    else platform.fs().getPath(uri.getPath)
}
