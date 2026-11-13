package org.jetbrains.plugins.scala.startup

import consulo.project.Project
import consulo.project.startup.BackgroundStartupActivity
import consulo.ui.UIAccess

/**
 * Scala shim around [[consulo.project.startup.BackgroundStartupActivity]]
 * (upstream: around com.intellij.openapi.startup.ProjectActivity to avoid exposing coroutine internals).
 */
trait ProjectActivity extends BackgroundStartupActivity {
  def execute(project: Project): Unit

  final override def runActivity(project: Project, uiAccess: UIAccess): Unit =
    execute(project)
}
