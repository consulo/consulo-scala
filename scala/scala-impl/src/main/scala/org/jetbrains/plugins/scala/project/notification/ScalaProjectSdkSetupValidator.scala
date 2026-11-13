package org.jetbrains.plugins.scala.project.notification

import com.intellij.codeInsight.daemon.ProjectSdkSetupValidator
import com.intellij.codeInsight.daemon.impl.JavaProjectSdkSetupValidator
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import com.intellij.ui.EditorNotificationPanel

final class ScalaProjectSdkSetupValidator extends ProjectSdkSetupValidator {
  private val JavaDelegate: JavaProjectSdkSetupValidator = JavaProjectSdkSetupValidator.INSTANCE

  override def isApplicableFor(project: Project, file: VirtualFile): Boolean = {
    isScalaSourceFile(file, project)
  }

  override def getErrorMessage(project: Project, file: VirtualFile): String =
    JavaDelegate.getErrorMessage(project, file)

  override def getFixHandler(project: Project, file: VirtualFile): EditorNotificationPanel.ActionHandler =
    JavaDelegate.getFixHandler(project, file)
}
