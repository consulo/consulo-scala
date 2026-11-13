package org.jetbrains.plugins.scala.statistics

import consulo.project.Project
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType

/**
 * Upstream reports these events to IntelliJ FUS (feature usage statistics) via `ScalaActionUsagesCollector` counter collector.
 * Consulo has no FUS event log, so logging is a no-op.
 */
object ScalaActionUsagesCollector {
  def logTypeInfo(project: Project): Unit = ()
  def logXRayMode(project: Project, duration: Long): Unit = ()
  def logShowImplicitParameters(project: Project): Unit = ()
  def logGoToImplicitConversion(project: Project): Unit = ()
  def logStructureView(project: Project): Unit = ()
  def logOptimizeImports(project: Project): Unit = ()
  def logCreateFromUsage(project: Project): Unit = ()
  def logOverrideImplement(project: Project): Unit = ()
  def logDesugarCode(project: Project): Unit = ()
  def logRearrange(project: Project): Unit = ()
  def logConvertFromJava(project: Project): Unit = ()
  def logRunWorksheet(project: Project): Unit = ()
  def logIncrementalityTypeSet(incrementalityType: IncrementalityType, project: Project): Unit = ()
}
