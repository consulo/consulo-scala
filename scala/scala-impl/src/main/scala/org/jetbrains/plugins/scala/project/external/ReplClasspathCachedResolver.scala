package org.jetbrains.plugins.scala.project.external

import consulo.project.Project
import consulo.platform.Platform
import org.jetbrains.plugins.scala.project.ReplClasspath

import scala.collection.mutable

/**
 * Holds already resolved REPL classpaths for different Scala versions. This is necessary because
 * the REPL classpath is a transitive dependency, which, unlike single jars, cannot be looked up directly
 * in the local Ivy cache. Every lookup therefore runs a full Ivy resolution, which may also hit the network.
 * This in-memory cache exploits the fact that the REPL classpath is stable for a given Scala version.
 * This helps us avoid re-resolving the REPL classpath for every module/subproject and cuts down on
 * network traffic.
 *
 * @note This class is '''not thread safe''', it contains mutable state and must be used from a single
 *       thread only.
 * @note This class is dependent on being garbage-collected. It is intended to be used as a short-lived
 *       cache during project imports only. It should not be referenced from long-lived components.
 *       It does not offer any APIs for cleaning up the state.
 */
//noinspection ApiStatus,UnstableApiUsage
// upstream: @ApiStatus.Internal
private[jetbrains] final class ReplClasspathCachedResolver(platform: Platform) {

  def this(project: Project) = this(Platform.current())

  private val cache: mutable.HashMap[String, ReplClasspath] = mutable.HashMap.empty

  def resolve(scalaVersion: String): ReplClasspath =
    cache.getOrElseUpdate(scalaVersion, ScalaSdkUtils.resolveReplClasspath(platform, scalaVersion))
}
