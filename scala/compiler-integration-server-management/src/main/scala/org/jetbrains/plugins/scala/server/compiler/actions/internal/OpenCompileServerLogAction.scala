package org.jetbrains.plugins.scala.server.compiler.actions.internal

import consulo.annotation.component.ActionImpl
import consulo.annotation.component.ActionParentRef
import consulo.annotation.component.ActionRef
import consulo.annotation.component.ActionRefAnchor
import consulo.language.psi.PsiNavigationSupport
import consulo.project.ui.notification.{Notification, NotificationType, Notifications}
import consulo.ui.ex.action.{ActionUpdateThread, AnActionEvent}
import consulo.application.ApplicationManager
import consulo.codeEditor.ScrollType
import consulo.fileEditor.{FileEditorManager, TextEditor}
import com.intellij.openapi.fileEditor.FileEditorOpenRequest
import com.intellij.openapi.project.DumbAwareAction
import consulo.project.Project
import consulo.util.lang.function.Condition
import consulo.virtualFileSystem.LocalFileSystem
import consulo.virtualFileSystem.util.VirtualFileUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.server.compiler.{CompileServerLauncher, ServerManagementBundle}
import org.jetbrains.plugins.scala.extensions.{IterableOnceExt, executeOnPooledThread, invokeLater}
import org.jetbrains.plugins.scala.server.CompileServerLog

import java.io.IOException
import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

/**
 * Similar to [[com.intellij.internal.OpenLogAction]].
 */
@ActionImpl(id = "Scala.OpenCompileServerLog", parents = Array(new ActionParentRef(value = new ActionRef(id = "HelpMenu"), anchor = ActionRefAnchor.BEFORE, relatedToAction = new ActionRef(id = "Scala.ShowCompileServerLog"))))
final class OpenCompileServerLogAction extends DumbAwareAction {

  // Called in the constructor
  getTemplatePresentation.setText(ServerManagementBundle.message("open.compile.server.log.action.text"))

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    if (project == null) return
    openLogInEditor(project)
  }

  override def update(e: AnActionEvent): Unit = {
    e.getPresentation.setEnabled(e.getProject != null)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  private def openLogInEditor(@NotNull project: Project): Unit = executeOnPooledThread {
    val logDir = CompileServerLauncher.logDirectory(project)
    val logFilePath = CompileServerLog.logFilePath(logDir)
    val file = LocalFileSystem.getInstance.refreshAndFindFileByNioFile(logFilePath)
    if (file != null) {
      VfsUtil.markDirtyAndRefresh(true, false, false, file)
      invokeLater {
        FileEditorManager.getInstance(project)
          .requestOpenFile(file, FileEditorOpenRequest.withFocus(true))
          .thenAccept { composite =>
            composite.getAllEditors.asScala.filterByType[TextEditor].headOption match {
              case Some(editor) =>
                FileEditorManager.getInstance(project).runWhenLoaded(editor.getEditor, () => {
                  scrollToLastIDEStart(editor)
                })
              case _ =>
                PsiNavigationSupport.getInstance.createNavigatable(project, file, -1).navigate(true)
            }
          }
      }
    } else {
      val title = s"Cannot find '$logFilePath'"
      val empty = ""
      Notifications.Bus.notify(
        new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, title, empty, NotificationType.INFORMATION): @nowarn("cat=deprecation"),
        project
      )
    }
  }

  private def scrollToLastIDEStart(editor: TextEditor): Unit = executeOnPooledThread {
    try {
      val length = editor.getFile.getLength
      val scrollOffset =
        if (length < 0) 0
        else if (length > Int.MaxValue.toLong) Int.MaxValue
        else length.toInt - 1

      val expire: Condition[?] = _ => editor.getEditor.isDisposed
      ApplicationManager.getApplication.invokeLater(() => {
        editor.getEditor.getCaretModel.moveToOffset(scrollOffset)
        editor.getEditor.getScrollingModel.scrollToCaret(ScrollType.CENTER_UP)
      }, expire)
    } catch {
      case _: IOException =>
    }
  }
}
