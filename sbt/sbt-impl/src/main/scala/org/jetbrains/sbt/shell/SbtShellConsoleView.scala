package org.jetbrains.sbt
package shell


import consulo.execution.internal.action.ClearConsoleAction
import com.intellij.java.execution.configurations.RemoteConnection
import consulo.ide.impl.idea.execution.console.LanguageConsoleImpl
import com.intellij.java.execution.filters.ExceptionFilter
import consulo.execution.ui.console.PatternBasedFileHyperlinkFilter
import consulo.execution.ui.console.PatternBasedFileHyperlinkRawDataFinder
import consulo.execution.ui.console.PatternHyperlinkFormat
import consulo.execution.ui.console.PatternHyperlinkPart
import consulo.execution.ui.console.UrlFilter
import consulo.ui.ex.action.{ActionGroup, AnAction, DefaultActionGroup}
import consulo.codeEditor.action.ToggleUseSoftWrapsToolbarAction
import consulo.codeEditor.event.EditorMouseEvent
import consulo.codeEditor.event.EditorMouseListener
import consulo.codeEditor.EditorEx
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.application.ui.wm.IdeFocusManager
import consulo.language.psi.scope.GlobalSearchScope
import org.jetbrains.plugins.scala.extensions.inWriteAction
import org.jetbrains.sbt.shell.action.*
import org.jetbrains.sbt.shell.optionsWarn.SbtShellOptionsWarningService

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.regex.Pattern
import scala.annotation.nowarn

final class SbtShellConsoleView private(project: Project, debugConnection: Option[RemoteConnection])
  extends LanguageConsoleImpl(project, SbtShellLanguage.getID, SbtShellLanguage) {

  def createActionGroup(): ActionGroup = {
    val group = new DefaultActionGroup()

    // hackery because we can't construct those actions directly
    val defaultActions = super.createConsoleActions()
    val toggleSoftWrapsAction = defaultActions.find(_.isInstanceOf[ToggleUseSoftWrapsToolbarAction])
      .getOrElse(throw new RuntimeException("action of type `ToggleUseSoftWrapsToolbarAction` couldn't be found"))

    //TODO: make it use the same shortcut as in Terminal (Cmd + K)
    // TODO: same for Scala REPL
    val clearAllAction = defaultActions.find(_.isInstanceOf[ClearConsoleAction])
      .getOrElse(throw new RuntimeException("action of type `ClearConsoleAction` couldn't be found"))

    val startAction = new StartAction(project)
    val stopAction = new StopAction(project)
    val debugShellAction = new DebugShellAction(project, debugConnection)
    val scrollToTheEndToolbarAction = new SbtShellScrollToTheEndToolbarAction(getEditor)
    val eofAction = new EOFAction(project)
    val copyFromHistoryViewerAction = new CopyFromHistoryViewerAction(this)
    val findAction = new FindAction(this)
    val escapeAction = new EscapeAction(this)

    val allActions: Array[AnAction] = Array(
      startAction,
      stopAction,
      debugShellAction,
      scrollToTheEndToolbarAction,
      toggleSoftWrapsAction,
      clearAllAction,
      eofAction,
      copyFromHistoryViewerAction,
      findAction,
      escapeAction
    )

    allActions.foreach { act =>
      act.registerCustomShortcutSet(act.getShortcutSet, this)
    }

    group.addAll(startAction, stopAction, debugShellAction)
    group.addSeparator()
    group.addAll(scrollToTheEndToolbarAction, toggleSoftWrapsAction, clearAllAction)
    group
  }

  override def dispose(): Unit = {
    SbtShellOptionsWarningService.instance(project).uninstallHistoryViewer(getHistoryViewer)
    super.dispose()
    ConsoleViewsRegistry.removeConsoleView(project)
  }

}

object SbtShellConsoleView {

  def apply(project: Project, debugConnection: Option[RemoteConnection]): SbtShellConsoleView = {
    // Use write action as a workaround for SCL-23073,
    // but I would expect AbstractConsoleRunnerWithHistory.initAndRun to use the read action
    // (the exception is about "read action" but "write action" is also needed for `ToolbarUpdater.updateActions`)
    val cv = inWriteAction {
      new SbtShellConsoleView(project, debugConnection)
    }

    // upstream: wrapped in WriteIntentReadAction in unit test mode (IJPL-27737)
    cv.getConsoleEditor.setOneLineMode(true)

    // stack trace file links
    cv.addMessageFilter(new ExceptionFilter(GlobalSearchScope.allScope(project)))
    // other file links
    cv.addMessageFilter(filePatternFilters(project))
    // url links
    cv.addMessageFilter(new UrlFilter(project))

    cv.getHistoryViewer.addEditorMouseListener(new HistoryMouseListener(cv))
    SbtShellOptionsWarningService.instance(project).installHistoryViewer(cv.getHistoryViewer)

    //in 2020.1 `updateUi` is invoked on toolwindow reopen, it reapplies LookAndFeel and adds default border to console editors
    forbidBorderFor(cv.getHistoryViewer)
    forbidBorderFor(cv.getConsoleEditor)

    ConsoleViewsRegistry.set(project, cv)

    cv
  }

  private def filePatternFilters(project: Project) = {
    import PatternHyperlinkPart._

    // upstream: the patterns are taken from `RegexpFilter.getPattern` with FILE_PATH_MACROS and LINE_MACROS,
    // these are the same regular expressions (Consulo's RegexpFilter doesn't expose its pattern)
    val filePathRegexp = "((?:\\p{Alpha}\\:)?[0-9 a-z_A-Z\\-\\\\./]+)"
    val lineRegexp = "([0-9]+)"
    def pattern(regexp: String) = Pattern.compile(regexp, Pattern.MULTILINE)

    // file with line number
    val fileWithLinePattern = pattern(s"$filePathRegexp:$lineRegexp")
    val fileWithLineFormat = new PatternHyperlinkFormat(fileWithLinePattern, false, false, PATH, LINE)

    // file output without lines in messages
    val fileOnlyPattern = pattern(filePathRegexp)
    val fileOnlyFormat = new PatternHyperlinkFormat(fileOnlyPattern, false, false, PATH)

    val dataFinder = new PatternBasedFileHyperlinkRawDataFinder(Array(fileWithLineFormat, fileOnlyFormat))
    new PatternBasedFileHyperlinkFilter(project, null, dataFinder)
  }

  private def forbidBorderFor(editor: EditorEx): Unit = {
    editor.getScrollPane.addPropertyChangeListener(new ResetBorderListener(editor))
  }

  /**
   * This listener resents the focus to console editor whenever history viewer is clicked.
   * This listener is a workaround  to IDEA-302621.
   * It was originally introduced within SCL-12392.
   * When IDEA-302621 is fixed we might get rid of this workaround.
   */
  private class HistoryMouseListener(cv: SbtShellConsoleView) extends EditorMouseListener {
    override def mouseClicked(e: EditorMouseEvent): Unit = {
      //we want to select content on double/triple click in history viewer
      //in this case don't resent focus
      val clickCount = e.getMouseEvent.getClickCount
      val historySelectionIsEmpty = !cv.getHistoryViewer.getSelectionModel.hasSelection //SCL-21593
      if (clickCount == 1 && historySelectionIsEmpty) {
        val focusManager = IdeFocusManager.getGlobalInstance
        val focusComponent = cv.getConsoleEditor.getContentComponent
        focusManager.doWhenFocusSettlesDown { () =>
          focusManager.requestFocus(focusComponent, false): Unit
        }: @nowarn("cat=deprecation")
      }
    }
  }

  private class ResetBorderListener(editor: EditorEx) extends PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit = {
      if (evt.getPropertyName == "border" && evt.getNewValue != null) {
        editor.getScrollPane.setBorder(null)
      }
    }
  }

}
