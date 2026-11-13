package org.jetbrains.plugins.scala.statistics

import consulo.project.Project

/**
 * Upstream reports these events to IntelliJ FUS (feature usage statistics) via `ScalaJsUsagesCollector` counter collector.
 * Consulo has no FUS event log, so logging is a no-op.
 */
object ScalaJsUsagesCollector {
  def logDynamicResolve(project: Project): Unit = ()
  def logDynamicCompletion(project: Project): Unit = ()
}
