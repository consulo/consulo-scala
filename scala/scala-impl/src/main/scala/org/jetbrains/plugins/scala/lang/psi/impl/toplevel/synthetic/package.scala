package org.jetbrains.plugins.scala.lang.psi.impl.toplevel

import consulo.project.Project
import consulo.module.content.ProjectRootManager
import consulo.component.util.SimpleModificationTracker

package object synthetic {

  /**
   * The method is extracted just to highlight that the same entity is used for caching for  syntenic elements of all types
   */
  private[toplevel] def projectLevelModificationTracker(project: Project): SimpleModificationTracker =
    ProjectRootManager.getInstance(project)
}
