package org.jetbrains.plugins.scala.compiler.sync

import consulo.dataContext.DataContext
import consulo.project.Project

trait SyncAction {
  def syncProject(project: Project, dataContext: DataContext): Unit
}
