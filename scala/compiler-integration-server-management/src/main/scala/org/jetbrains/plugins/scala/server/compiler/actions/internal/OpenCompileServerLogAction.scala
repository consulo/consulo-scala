package org.jetbrains.plugins.scala.server.compiler.actions.internal

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor
import consulo.language.psi.PsiNavigationSupport
import consulo.localize.LocalizeValue
import consulo.project.ui.notification.{NotificationGroup, NotificationService, NotificationType}
import consulo.ui.ex.action.{ActionUpdateThread, AnActionEvent, AnActionWithSyncUpdate, DumbAwareAction}
import consulo.application.ApplicationManager
import consulo.codeEditor.ScrollType
import consulo.fileEditor.{FileEditorManager, TextEditor}
import consulo.project.Project
import consulo.virtualFileSystem.LocalFileSystem
import consulo.virtualFileSystem.util.VirtualFileUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.server.compiler.{CompileServerLauncher, ServerManagementBundle}
import org.jetbrains.plugins.scala.extensions.{executeOnPooledThread, invokeLater}
import org.jetbrains.plugins.scala.server.CompileServerLog

import java.io.IOException

/**
 * Similar to [[com.intellij.internal.OpenLogAction]].
 */
// upstream: internal="true"
@ActionImpl(id = "Scala.OpenCompileServerLog", parents = Array(new ActionParentRef(value = new ActionRef(id = "HelpMenu"), anchor = ActionRefAnchor.BEFORE, relatedToAction = new ActionRef(id = "Scala.ShowCompileServerLog"))))
final class OpenCompileServerLogAction extends DumbAwareAction(LocalizeValue.of(ServerManagementBundle.message("open.compile.server.log.action.text"))) with AnActionWithSyncUpdate {

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getData(Project.KEY)
    if (project == null) return
    openLogInEditor(project)
  }

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    presentation.setVisible(ApplicationManager.getApplication.isInternal)
    presentation.setEnabled(e.getData(Project.KEY) != null)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  private def openLogInEditor(@NotNull project: Project): Unit = executeOnPooledThread {
    val logDir = CompileServerLauncher.logDirectory(project)
    val logFilePath = CompileServerLog.logFilePath(logDir)
    val file = LocalFileSystem.getInstance.refreshAndFindFileByNioFile(logFilePath)
    if (file != null) {
      VirtualFileUtil.markDirtyAndRefresh(true, false, false, file)
      invokeLater {
        // upstream: FileEditorManager.requestOpenFile(file, FileEditorOpenRequest.withFocus(true)) + runWhenLoaded
        FileEditorManager.getInstance(project).openFile(file, true).collectFirst { case editor: TextEditor => editor } match {
          case Some(editor) =>
            scrollToLastIDEStart(editor)
          case _ =>
            PsiNavigationSupport.getInstance.createNavigatable(project, file, -1).navigate(true)
        }
      }
    } else {
      val title = s"Cannot find '$logFilePath'"
      NotificationService.getInstance
        .newOfType(NotificationGroup.SYSTEM_MESSAGES, NotificationType.INFORMATION)
        .title(LocalizeValue.of(title))
        .notify(project)
    }
  }

  private def scrollToLastIDEStart(editor: TextEditor): Unit = executeOnPooledThread {
    try {
      val length = editor.getFile.getLength
      val scrollOffset =
        if (length < 0) 0
        else if (length > Int.MaxValue.toLong) Int.MaxValue
        else length.toInt - 1

      ApplicationManager.getApplication.invokeLater(() => {
        editor.getEditor.getCaretModel.moveToOffset(scrollOffset)
        editor.getEditor.getScrollingModel.scrollToCaret(ScrollType.CENTER_UP)
      }, () => editor.getEditor.isDisposed)
    } catch {
      case _: IOException =>
    }
  }
}
