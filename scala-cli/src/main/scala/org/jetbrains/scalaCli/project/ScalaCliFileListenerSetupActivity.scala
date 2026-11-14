package org.jetbrains.scalaCli.project

import consulo.annotation.component.ExtensionImpl

import consulo.project.Project
import consulo.virtualFileSystem.VirtualFileManager
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.startup.ProjectActivity

@ExtensionImpl
class ScalaCliFileListenerSetupActivity extends ProjectActivity {
  override def execute(project: Project): Unit = {
    val fileListener = new ScalaCliFileListener(project)
    VirtualFileManager.getInstance().addAsyncFileListener(fileListener, project.unloadAwareDisposable)
  }
}
