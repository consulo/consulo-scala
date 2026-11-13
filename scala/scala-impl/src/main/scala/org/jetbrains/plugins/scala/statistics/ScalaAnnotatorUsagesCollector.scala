package org.jetbrains.plugins.scala.statistics

import consulo.project.Project

/**
 * Upstream reports these events to IntelliJ FUS (feature usage statistics) via `ScalaAnnotatorUsagesCollector` counter collector.
 * Consulo has no FUS event log, so logging is a no-op.
 */
object ScalaAnnotatorUsagesCollector {
  def logTypeAware(project: Project): Unit = ()
  def logNoneTypeAware(project: Project): Unit = ()
  def logStructuralType(project: Project): Unit = ()
  def logExistentialType(project: Project): Unit = ()
  def logMacroDefinition(project: Project): Unit = ()
  def logCollectionTypeHighlighting(project: Project): Unit = ()
}
