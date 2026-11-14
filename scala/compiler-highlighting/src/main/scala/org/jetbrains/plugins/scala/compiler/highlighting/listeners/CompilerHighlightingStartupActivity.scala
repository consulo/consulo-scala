package org.jetbrains.plugins.scala.compiler.highlighting.listeners

import consulo.annotation.component.ExtensionImpl
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.project.startup.PostStartupActivity
import consulo.ui.UIAccess

/**
 * Registers the listeners which have no extension point or topic in Consulo
 */
@ExtensionImpl
final class CompilerHighlightingStartupActivity extends PostStartupActivity with DumbAware {
  override def runActivity(project: Project, uiAccess: UIAccess): Unit = {
    if (!project.isDisposed) {
      CompilerHighlightingPsiChangeListener.register(project)
    }
  }
}
