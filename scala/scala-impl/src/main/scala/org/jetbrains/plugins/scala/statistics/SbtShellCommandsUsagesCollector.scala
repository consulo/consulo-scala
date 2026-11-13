package org.jetbrains.plugins.scala.statistics

import consulo.project.Project

/**
 * Upstream reports these events to IntelliJ FUS (feature usage statistics) via `SbtShellCommandsUsagesCollector` counter collector.
 * Consulo has no FUS event log, so logging is a no-op.
 */
object SbtShellCommandsUsagesCollector {
  def logShellCommand(project: Project): Unit = ()
  def logShellTestCommand(project: Project): Unit = ()
  def logShellTestRunCommand(project: Project): Unit = ()
}
