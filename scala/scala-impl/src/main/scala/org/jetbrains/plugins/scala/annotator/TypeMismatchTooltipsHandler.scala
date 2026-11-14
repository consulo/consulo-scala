package org.jetbrains.plugins.scala.annotator

import consulo.annotation.component.ExtensionImpl
import consulo.codeEditor.event.{EditorMouseEvent, EditorMouseMotionListener}
import consulo.codeEditor.{Editor, EditorFactory, EditorMouseHoverPopupControl}
import consulo.language.editor.DaemonCodeAnalyzer
import consulo.language.editor.rawHighlight.HighlightInfo
import consulo.project.Project
import org.jetbrains.plugins.scala.annotator.TypeMismatchError.TypeMismatchErrorProblemGroup
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.startup.ProjectActivity

// Annotation.setTooltip is shown both on mouse hover (EditorMouseHoverPopupManager), and on Ctrl / Cmd + F1 (ShowErrorDescriptionAction)
// But when type mismatch hints are enabled, we don't want tooltips on mouse hover (on the expression), but still want tooltips on the explicit action.
// https://youtrack.jetbrains.com/issue/SCL-15782
@ExtensionImpl
final class TypeMismatchTooltipsHandler extends ProjectActivity {
  override def execute(project: Project): Unit = {
    val listener = new TypeMismatchTooltipsHandler.Listener(project)
    EditorFactory.getInstance().getEventMulticaster.addEditorMouseMotionListener(listener, project.unloadAwareDisposable)
  }
}

private object TypeMismatchTooltipsHandler {
  private class Listener(project: Project) extends EditorMouseMotionListener {
    // See consulo.codeEditor.EditorMouseHoverPopupControl
    private var popupsWereDisabled = false

    override def mouseMoved(e: EditorMouseEvent): Unit = {
      if (!e.isConsumed && project.isInitialized && !project.isDisposed) {
        val editor = e.getEditor

        if (ScalaProjectSettings.getInstance(project).isTypeMismatchHints) {
          val offset = e.getOffset

          val highlightInfo = inReadAction {
            Option(DaemonCodeAnalyzer.getInstance(project).findHighlightByOffset(editor.getDocument, offset, false))
          }

          disableTooltipOnMouseHoverForTypeMismatchErrors(editor, highlightInfo)
        } else {
          if (popupsWereDisabled) {
            EditorMouseHoverPopupControl.enablePopups(editor);
            popupsWereDisabled = false
          }
        }
      }
    }

    private def disableTooltipOnMouseHoverForTypeMismatchErrors(editor: Editor, maybeInfo: Option[HighlightInfo]): Unit = {
      val isTypeMismatchError = maybeInfo.exists(_.getProblemGroup == TypeMismatchErrorProblemGroup)

      if (isTypeMismatchError) {
        if (!popupsWereDisabled && !EditorMouseHoverPopupControl.arePopupsDisabled(editor)) {
          EditorMouseHoverPopupControl.disablePopups(editor);
          popupsWereDisabled = true
        }
      } else {
        if (popupsWereDisabled) {
          EditorMouseHoverPopupControl.enablePopups(editor);
          popupsWereDisabled = false
        }
      }
    }

  }
}
