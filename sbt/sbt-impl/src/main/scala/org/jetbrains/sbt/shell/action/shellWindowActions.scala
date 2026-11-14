package org.jetbrains.sbt.shell.action

import consulo.platform.base.icon.PlatformIconGroup

import com.intellij.java.debugger.impl.DebuggerManagerEx
import com.intellij.java.debugger.impl.engine.RemoteDebugProcessHandler
import com.intellij.java.debugger.impl.DebuggerSession
import com.intellij.java.execution.configurations.RemoteConnection
import consulo.execution.debug.DefaultDebugExecutor
import consulo.execution.debug.icon.ExecutionDebugIconGroup
import consulo.process.{ProcessHandler, ProcessHandlerStopper}
import com.intellij.java.execution.impl.remote.RemoteConfiguration
import com.intellij.java.execution.impl.remote.RemoteConfigurationType
import consulo.execution.runner.ExecutionEnvironmentBuilder
import consulo.execution.ui.RunContentDescriptor
import consulo.execution.ExecutionManager
import consulo.execution.ProgramRunnerUtil
import consulo.execution.RunManager
import consulo.execution.RunnerAndConfigurationSettings
import consulo.ide.impl.idea.find.EditorSearchSession
import consulo.find.FindModel
import consulo.ide.impl.idea.find.FindUtil
import consulo.find.FindManager
import consulo.ui.ex.action.{ActionManager, ActionUpdateThread, AnActionEvent, AnActionWithSyncUpdate, CustomShortcutSet, IdeActions}
import consulo.ui.event.details.{KeyCode, KeyStroke}
import consulo.ui.event.details.ModifiedInputDetails.Modifier
import consulo.ui.ex.action.CommonShortcuts
import consulo.ui.ex.action.Presentation
import consulo.ui.ex.action.ToggleAction
import consulo.execution.action.ScrollToTheEndToolbarAction
import consulo.codeEditor.{Editor, SelectionModel}
import consulo.ui.ex.action.DumbAwareAction
import consulo.project.Project
import consulo.application.ui.wm.IdeFocusManager
import consulo.ui.UIAccess
import org.jetbrains.plugins.scala.extensions.{ObjectExt, executeOnPooledThread}
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.shell.action.SbtShellActionUtil.shellAlive
import org.jetbrains.sbt.shell.{SbtProcessManager, SbtShellCommunication, SbtShellConsoleView, SbtShellToolWindowFactory}

import scala.jdk.CollectionConverters.*

//TODO: Split this file into multiple files

final class SbtShellScrollToTheEndToolbarAction(editor: Editor) extends ScrollToTheEndToolbarAction(editor) {

  private val end = KeyStroke.pressed(KeyCode.END, Modifier.CTRL)
  private val shortcuts = new CustomShortcutSet(end)
  setShortcutSet(shortcuts)
}

/**
  * Starts or restarts sbt shell depending on running state.
  */
final class StartAction(project: Project) extends DumbAwareAction with AnActionWithSyncUpdate {
  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  copyShortcutFrom(ActionManager.getInstance.getAction(IdeActions.ACTION_DEFAULT_RUNNER))
  private val templatePresentation: Presentation = getTemplatePresentation
  templatePresentation.setIcon(PlatformIconGroup.actionsExecute())
  templatePresentation.setText(SbtBundle.message("sbt.shell.start"))

  override def actionPerformed(e: AnActionEvent): Unit =
    SbtShellToolWindowFactory.instance(using project).foreach { toolWindow =>
      toolWindow.getContentManager.removeAllContents(true)

      executeOnPooledThread {
        SbtProcessManager.forProject(project).restartProcess()
      }
    }

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    if (shellAlive(project)) {
      presentation.setIcon(PlatformIconGroup.actionsRestart())
      presentation.setText(SbtBundle.message("sbt.shell.restart"))
    } else {
      presentation.setIcon(PlatformIconGroup.actionsExecute())
      presentation.setText(SbtBundle.message("sbt.shell.start"))
    }
  }

}

final class StopAction(project: Project) extends DumbAwareAction with AnActionWithSyncUpdate {
  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  copyFrom(ActionManager.getInstance.getAction(IdeActions.ACTION_STOP_PROGRAM))
  private val templatePresentation: Presentation = getTemplatePresentation
  templatePresentation.setIcon(PlatformIconGroup.actionsSuspend())
  templatePresentation.setText(SbtBundle.message("sbt.shell.stop"))
  templatePresentation.setDescription(null: String)

  override def actionPerformed(e: AnActionEvent): Unit =
    if (isEnabled) {
      executeOnPooledThread {
        SbtProcessManager.forProject(project).destroyProcess()
      }
    }

  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setEnabled(isEnabled)


  private def isEnabled: Boolean = shellAlive(project)
}

final class EOFAction(project: Project) extends DumbAwareAction {

  private val templatePresentation: Presentation = getTemplatePresentation
  templatePresentation.setIcon(PlatformIconGroup.actionsTraceover()) // TODO sensible icon
  templatePresentation.setText(SbtBundle.message("sbt.shell.ctrl.d.eof"))
  //noinspection ScalaExtractStringToBundle
  templatePresentation.setDescription("")

  private val ctrlD = KeyStroke.pressed(KeyCode.D, Modifier.CTRL)
  private val shortcuts = new CustomShortcutSet(ctrlD)
  setShortcutSet(shortcuts)

  override def actionPerformed(e: AnActionEvent): Unit =
    executeOnPooledThread {
      SbtShellCommunication.forProject(project).send("\u0004")
    }
}

final class CopyFromHistoryViewerAction(view: SbtShellConsoleView) extends DumbAwareAction with AnActionWithSyncUpdate {
  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT


  setShortcutSet(CommonShortcuts.getCopy)

  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setEnabled(CopyFromHistoryViewerAction.isEnabled(e, view))


  override def actionPerformed(e: AnActionEvent): Unit =
    CopyFromHistoryViewerAction.copyFromHistoryToClipboard(e, view)
}

private object CopyFromHistoryViewerAction {
  def selectionModel(e: AnActionEvent, view: SbtShellConsoleView): SelectionModel = {
    //we have two editors in sbt-shell: console and history viewer
    //we try to use selection from the focused editor, if we have any
    val editorInFocus = e.getData(Editor.KEY)
    val editor = if (editorInFocus != null) editorInFocus else view.getHistoryViewer
    editor.getSelectionModel
  }

  def isEnabled(e: AnActionEvent, view: SbtShellConsoleView): Boolean =
    selectionModel(e, view).hasSelection

  private def copyFromHistoryToClipboard(e: AnActionEvent, view: SbtShellConsoleView): Unit =
    selectionModel(e, view).copySelectionToClipboard()
}

final class FindAction(view: SbtShellConsoleView) extends DumbAwareAction {
  setShortcutSet(CommonShortcuts.getFind)

  /**
   * The implementation was inspired by original "Find" action logic from
   * [[com.intellij.openapi.editor.actions.IncrementalFindAction.Handler]]
   */
  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = view.getProject
    val historyViewer = view.getHistoryViewer

    val editorInContext = e.getData(Editor.KEY)
    val editorInFocus = if (editorInContext != null) editorInContext else view.getHistoryViewer

    //we don't need "replace" feature for history viewer because it's not modifiable
    //note: it's still possible to replace the content when you invoke "replace" in "find" widget, due to this bug: IDEA-302629
    val replace = false

    val search = EditorSearchSession.get(historyViewer)
    if (search != null) {
      val model = search.getFindModel
      FindUtil.configureFindModel(replace, editorInFocus, model, false)
      search.getComponent.requestFocusInTheSearchFieldAndSelectContent(project)
    }
    else {
      val findManager = FindManager.getInstance(project)
      val model = new FindModel
      model.copyFrom(findManager.getFindInFileModel)

      FindUtil.configureFindModel(replace, editorInFocus, model, true)

      EditorSearchSession.start(historyViewer, model, project).whenCompleteAsync(
        (search, _) => if (search != null) search.getComponent.requestFocusInTheSearchFieldAndSelectContent(project),
        UIAccess.current()
      )
    }
  }
}

/**
 * This action is needed to be able to close search panel in history viewer when it's open or remove selection in history viewer.
 * In a usual situation, history viewer should do that automatically.
 * However if we manually change focus to the console editor, it won't work so we need custom action for that.
 */
final class EscapeAction(view: SbtShellConsoleView) extends DumbAwareAction with AnActionWithSyncUpdate {
  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT

  setShortcutSet(CommonShortcuts.ESCAPE)


  //We want this action to only active when there is an active search in history viewer otherwise we want the default
  // behaviour for the ESC button (e.g. loose focus from the tool window and set focus to the code editor)
  override def update(e: AnActionEvent): Unit = {
    val historyViewer = view.getHistoryViewer
    val historyViewerHasSearchSession = EditorSearchSession.get(historyViewer) != null
    val historyViewerIsFocused = IdeFocusManager.getGlobalInstance.getFocusedDescendantFor(historyViewer.getComponent) != null
    val enabled = historyViewerHasSearchSession || historyViewerIsFocused
    e.getPresentation.setEnabled(enabled)
  }

  override def actionPerformed(e: AnActionEvent): Unit = {
    val searchSession = EditorSearchSession.get(view.getEditor)
    if (searchSession != null) {
      searchSession.close()
    }

    //Ensure that focus is set on console editor, without this call even if the console editor is focused,
    // the focus will jump to the history viewer due to implementation of EditorSearchSession.close
    view.getHistoryViewer.getSelectionModel.removeSelection()
    IdeFocusManager.getGlobalInstance.requestFocus(view.getConsoleEditor.getContentComponent, false)
  }
}

final class DebugShellAction(project: Project, remoteConnection: Option[RemoteConnection]) extends ToggleAction {
  copyShortcutFrom(ActionManager.getInstance.getAction(IdeActions.ACTION_DEFAULT_DEBUGGER))
  private val templatePresentation: Presentation = getTemplatePresentation
  templatePresentation.setIcon(ExecutionDebugIconGroup.actionStartdebugger())
  if (remoteConnection.isDefined) {
    templatePresentation.setText(SbtBundle.message("sbt.shell.attach.debugger"))
  } else {
    templatePresentation.setText(SbtBundle.message("sbt.shell.enable.debugging.in.sbt.settings"))
  }

  private val configType = new RemoteConfigurationType
  private val configName = SbtBundle.message("sbt.shell.debug")

  override def setSelected(e: AnActionEvent, toSet: Boolean): Unit = {
    val active = isSelected(e)
    if (toSet && !active) attach()
    else if (!toSet && active) detach()
  }

  override def isSelected(e: AnActionEvent): Boolean = {
    findSession.fold(false) { session => session.isAttached || session.isConnecting }
  }

  private def attach(): Unit = remoteConnection.foreach { remote =>

    val runManager = RunManager.getInstance(project)

    // upstream: the temporary configuration also doesn't activate the tool window before run and is not allowed to run in parallel
    val runConfig =
      findRunConfig
        .getOrElse {
          val rc = runManager.createRunConfiguration(configName, configType.getConfigurationFactories()(0))
          rc.setTemporary(true)
          runManager.addConfiguration(rc)
          rc
        }

    val options = runConfig.getConfiguration.asInstanceOf[RemoteConfiguration].getOptions
    options.setPort(remote.getAddress)
    options.setHost(remote.getHostName)
    options.setServerMode(remote.isServerMode)
    options.setUseSocketTransport(remote.isUseSockets)

    val environmentBuilder = ExecutionEnvironmentBuilder.create(DefaultDebugExecutor.getDebugExecutorInstance, runConfig)
    ProgramRunnerUtil.executeConfiguration(environmentBuilder.build(), false, false)
  }

  private def detach(): Unit = {
    val descriptors = ExecutionManager.getInstance(project).getContentManager.getAllDescriptors

    // This is pretty hacky, is there a cleaner way to handle the detaching?
    for {
      proc <- findProcessHandlerByNameAndType(descriptors.asScala)
    } ProcessHandlerStopper.stop(proc)
  }

  private def findProcessHandlerByNameAndType(descriptors: Iterable[RunContentDescriptor]): Option[ProcessHandler] =
    for {
      desc <- descriptors.find(_.getDisplayName == configName)
      proc <- Option(desc.getProcessHandler)
      if proc.is[RemoteDebugProcessHandler]
      if !(proc.isProcessTerminated || proc.isProcessTerminating)
    } yield proc

  private def findRunConfig: Option[RunnerAndConfigurationSettings] = {
    val runManager = RunManager.getInstance(project)
    Option(runManager.findConfigurationByTypeAndName(configType.getId, configName))
  }

  private def findSession: Option[DebuggerSession] = {
    val sessions = DebuggerManagerEx.getInstanceEx(project).getSessions
    sessions.asScala.find(_.getSessionName == configName)
  }

  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setEnabled(isEnabled)


  private def isEnabled: Boolean = remoteConnection.isDefined && shellAlive(project)
}

private object SbtShellActionUtil {
  def shellAlive(project: Project): Boolean = SbtProcessManager.forProject(project).isAlive
}
