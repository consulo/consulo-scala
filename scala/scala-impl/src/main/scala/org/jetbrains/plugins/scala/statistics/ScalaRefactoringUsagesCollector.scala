package org.jetbrains.plugins.scala.statistics

import consulo.project.Project

/**
 * Upstream reports these events to IntelliJ FUS (feature usage statistics) via `ScalaRefactoringUsagesCollector` counter collector.
 * Consulo has no FUS event log, so logging is a no-op.
 */
object ScalaRefactoringUsagesCollector {
  def logRenameLocal(project: Project): Unit = ()
  def logRenameMember(project: Project): Unit = ()
  def logMoveFile(project: Project): Unit = ()
  def logMoveClass(project: Project): Unit = ()
  def logIntroduceVariable(project: Project): Unit = ()
  def logIntroduceTypeAlias(project: Project): Unit = ()
  def logIntroduceFiled(project: Project): Unit = ()
  def logIntroduceParameter(project: Project): Unit = ()
  def logExtractMethod(project: Project): Unit = ()
  def logExtractTrait(project: Project): Unit = ()
  def logInline(project: Project): Unit = ()
  def logChangeSignature(project: Project): Unit = ()
}
