package org.jetbrains.plugins.scala.editor

import consulo.fileEditor.EditorTabTitleProvider
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile

class PackageObjectEditorTabTitleProvider extends EditorTabTitleProvider {
  override def getEditorTabTitle(project: Project, file: VirtualFile): String = {
    file.getName match {
      case "package.scala" if file.getParent != null =>
        val dirName = file.getParent.getName
        s"$dirName${java.io.File.separator}package.scala"
      case _ => null
    }
  }
}
