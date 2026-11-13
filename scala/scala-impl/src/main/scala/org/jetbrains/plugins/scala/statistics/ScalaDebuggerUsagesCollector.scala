package org.jetbrains.plugins.scala.statistics

import consulo.project.Project

/**
 * Upstream reports these events to IntelliJ FUS (feature usage statistics) via `ScalaDebuggerUsagesCollector` counter collector.
 * Consulo has no FUS event log, so logging is a no-op.
 */
object ScalaDebuggerUsagesCollector {
  def logDebugger(project: Project): Unit = ()
  def logEvaluator(project: Project): Unit = ()
  def logCompilingEvaluator(project: Project): Unit = ()
  def logLambdaBreakpoint(project: Project): Unit = ()
  def logSmartStepInto(project: Project): Unit = ()
}
