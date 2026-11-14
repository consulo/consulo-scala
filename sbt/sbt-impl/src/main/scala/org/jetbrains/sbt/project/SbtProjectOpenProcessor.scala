package org.jetbrains.sbt
package project

import consulo.annotation.component.ExtensionImpl


import org.jetbrains.sbt.api.file
import com.intellij.ide.impl.ProjectUtilKt
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor

import javax.swing.Icon
import kotlin.coroutines.Continuation

@ExtensionImpl
class SbtProjectOpenProcessor extends ProjectOpenProcessor {

  //noinspection ReferencePassedToNls
  override def getName: String = Sbt.Name
  override def getIcon: Icon = Sbt.Icon

  override def canOpenProject(file: VirtualFile): Boolean =
    SbtProjectImportProvider.canImport(file)

  override def openProjectAsync(virtualFile: VirtualFile,
                                projectOpenOptions: ProjectOpenProcessor.ProjectOpenOptions,
                                continuation: Continuation[? >: Project]): AnyRef =
    new SbtOpenProjectProvider().openProject(
      virtualFile,
      ProjectUtilKt.toOpenProjectTask(projectOpenOptions),
      continuation
    )
}
