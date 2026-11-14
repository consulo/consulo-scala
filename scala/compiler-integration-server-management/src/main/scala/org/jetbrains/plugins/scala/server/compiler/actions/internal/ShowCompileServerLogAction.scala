package org.jetbrains.plugins.scala.server.compiler.actions.internal

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor
import consulo.application.ApplicationManager
import consulo.localize.LocalizeValue
import consulo.platform.{Platform, PlatformFeature}
import consulo.ui.UIAccess
import consulo.ui.ex.action.{ActionUpdateThread, AnActionEvent, AnActionWithSyncUpdate, DumbAwareAction}
import consulo.project.Project
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.server.compiler.{CompileServerLauncher, ServerManagementBundle}
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.server.CompileServerLog

/**
 * Similar to [[consulo.ide.impl.idea.ide.actions.ShowLogAction]].
 */
// upstream: internal="true", synonyms "show.compile.server.log.synonym.finder"/"show.compile.server.log.synonym.explorer"
@ActionImpl(id = "Scala.ShowCompileServerLog", parents = Array(new ActionParentRef(value = new ActionRef(id = "HelpMenu"), anchor = ActionRefAnchor.AFTER, relatedToAction = new ActionRef(id = "ShowLog"))))
final class ShowCompileServerLogAction extends DumbAwareAction with AnActionWithSyncUpdate {

  getTemplatePresentation.setText(LocalizeValue.of(displayName))

  override def actionPerformed(e: AnActionEvent): Unit = {
    showLog(e.getData(Project.KEY), UIAccess.current())
  }

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    presentation.setVisible(ApplicationManager.getApplication.isInternal && isSupported)
    presentation.setText(LocalizeValue.of(displayName))
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  private def displayName: String =
    ServerManagementBundle.message("show.compile.server.log.action.text", Platform.current().fileManagerName())

  private def isSupported: Boolean =
    Platform.current().supportsFeature(PlatformFeature.OPEN_FILE_IN_FILE_MANAGER)

  private def showLog(@Nullable project: Project, uiAccess: UIAccess): Unit = {
    val logDir = CompileServerLauncher.logDirectory(project)
    val logFile = CompileServerLog.logFilePath(logDir)
    // upstream: RevealFileAction.openFile / openDirectory
    if (logFile.exists) Platform.current().openFileInFileManager(logFile, uiAccess)
    else Platform.current().openDirectoryInFileManager(logDir, uiAccess)
  }
}
