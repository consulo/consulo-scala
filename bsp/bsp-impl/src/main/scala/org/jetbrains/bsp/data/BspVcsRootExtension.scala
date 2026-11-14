package org.jetbrains.bsp.data

import consulo.annotation.component.{ComponentScope, ExtensionAPI}
import consulo.project.Project
import org.jetbrains.plugins.scala.ExtensionPointDeclaration

// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
trait BspVcsRootExtension {
  def onVcsRootAdded(project: Project): Unit
}

object BspVcsRootExtension extends ExtensionPointDeclaration[BspVcsRootExtension]("com.intellij.bspVcsRootExtension") {
  def onVcsRootAdded(project: Project): Unit = {
    implementations.foreach(_.onVcsRootAdded(project))
  }
}
