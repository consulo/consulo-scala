package org.jetbrains.plugins.scala.bazel

import consulo.annotation.component.ExtensionImpl
import consulo.dataContext.DataContext
import consulo.project.Project
import org.jetbrains.plugins.scala.actions.NewScalaFileActionExtension

@ExtensionImpl
final class BazelNewScalaFileActionExtension extends NewScalaFileActionExtension {
  override def isAvailable(dataContext: DataContext): Boolean = {
    val project = dataContext.getData(Project.KEY)
    if (project == null) return false
    org.jetbrains.bazel.config.project.isBazelProject(project)
  }
}
