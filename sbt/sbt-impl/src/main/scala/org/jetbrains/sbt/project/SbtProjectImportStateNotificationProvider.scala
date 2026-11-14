package org.jetbrains.sbt.project

import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.ExtensionImpl
import consulo.fileEditor.{EditorNotificationBuilder, EditorNotificationProvider, FileEditor}
import consulo.language.psi.PsiManager
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.Inject
import org.jetbrains.annotations.Nullable
import org.jetbrains.sbt.api.language.SbtFile
import org.jetbrains.sbt.codeInsight.daemon.SbtProblemHighlightFilter

import java.util.function.Supplier

@ExtensionImpl
private final class SbtProjectImportStateNotificationProvider @Inject()(project: Project) extends EditorNotificationProvider {

  override def getId: String = "sbt-project-import-state"

  @RequiredReadAction
  @Nullable
  override def buildNotification(file: VirtualFile, fileEditor: FileEditor, supplier: Supplier[EditorNotificationBuilder]): EditorNotificationBuilder = {
    val psiFile = PsiManager.getInstance(project).findFile(file)
    val sbtFile = psiFile match {
      case f: SbtFile => f
      case _ =>
        // Not an sbt file, do not show any notifications.
        return null
    }
    // The sbt file is not a part of the current project, do not show any notifications.
    if (!SbtProblemHighlightFilter.shouldHighlightSbtFile(sbtFile)) return null
    // upstream: no notification while the project reload is in progress (IncompleteModelUtil.isIncompleteModel) - not in Consulo
    // The project is fully imported, do not show any notifications.
    if (SbtProjectImportStateService.instance(project).isImported(sbtFile)) return null

    SbtProjectImportStateNotificationPanel.fillNotification(project, supplier.get())
  }
}
