package org.jetbrains.plugins.scala.codeInsight.hints

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.ProblemHighlightFilter
import consulo.language.editor.highlight.HighlightingLevelManager
import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.ex.action.{AnAction, AnActionEvent, AnActionWithSyncUpdate, AnSeparator, DefaultActionGroup, DumbAwareAction}
import consulo.codeEditor.Editor
import consulo.codeEditor.markup.InspectionWidgetActionProvider
import consulo.language.psi.PsiFile
import consulo.localize.LocalizeValue
import consulo.module.content.ProjectFileIndex
import consulo.ui.image.ImageEffects
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings.{getInstance => ScalaApplicationSettings}
import org.jetbrains.plugins.scala.settings.XRayWidgetMode

// upstream: the action is rendered by a custom ActionButtonWithText component (smaller font, HelpTooltip with
// a link to the X-Ray settings, Ctrl/Cmd+click pass-through); Consulo: a plain action with the tooltip as description
@ExtensionImpl
class XRayModeWidgetActionProvider extends InspectionWidgetActionProvider {
  override def createAction(editor: Editor): AnAction = {
    val project = editor.getProject
    if (project == null || project.isDefault) return null

    val file = editor.getVirtualFile
    if (file == null || file.getExtension != "scala" && file.getExtension != "sc" && file.getExtension != "sbt" && file.getExtension != "mill") return null

    val action = new DumbAwareAction(LocalizeValue.of(ScalaCodeInsightBundle.message("xray.mode.widget.text"))) with AnActionWithSyncUpdate {

      override def update(e: AnActionEvent): Unit = {
        val presentation = e.getPresentation
        if (ScalaHintsSettings.xRayMode) {
          presentation.setText(LocalizeValue.of(ScalaCodeInsightBundle.message("xray.mode.widget.text")))
          presentation.setIcon(
            if (ScalaHintsSettings.xRayModePinned) Icons.PIN
            else ImageEffects.grayed(Icons.PIN))
        } else {
          presentation.setText(LocalizeValue.empty())
          presentation.setIcon(ImageEffects.grayed(PlatformIconGroup.actionsShow()))
        }
        val tooltip =
          if (ScalaHintsSettings.xRayMode)
            if (ScalaHintsSettings.xRayModePinned) ScalaCodeInsightBundle.message("xray.mode.widget.tooltip.exit")
            else ScalaCodeInsightBundle.message("xray.mode.widget.tooltip.pin")
          else
            ScalaCodeInsightBundle.message("xray.mode.widget.tooltip.enter") + " (" + ScalaHintsSettings.xRayModeShortcut + ")"
        presentation.setDescription(LocalizeValue.of(tooltip))
      }

      override def actionPerformed(e: AnActionEvent): Unit = {
        if (ScalaHintsSettings.xRayMode) {
          if (ScalaHintsSettings.xRayModePinned) {
            ScalaEditorFactoryListener.setXRayModeEnabled(false, e.getData(Editor.KEY))
          }
          ScalaHintsSettings.xRayModePinned = !ScalaHintsSettings.xRayModePinned
        } else {
          ScalaEditorFactoryListener.setXRayModeEnabled(true, e.getData(Editor.KEY))
          ScalaHintsSettings.xRayModePinned = true
        }
      }
    }

    new DefaultActionGroup(action, AnSeparator.create()) with AnActionWithSyncUpdate {

      override def update(e: AnActionEvent): Unit = {
        def inLibrarySource = Option(e.getData(Editor.KEY)).exists { editor =>
          Option(editor.getVirtualFile).exists(file => ProjectFileIndex.getInstance(editor.getProject).isInLibrarySource(file))
        }
        def shouldHighlight = Option(e.getData(PsiFile.KEY)).exists { psiFile =>
          HighlightingLevelManager.getInstance(project).shouldHighlight(psiFile) &&
            ProblemHighlightFilter.shouldHighlightFile(psiFile)
        }
        val visible = ScalaApplicationSettings.XRAY_WIDGET_MODE match {
          case XRayWidgetMode.ALWAYS => !inLibrarySource && shouldHighlight
          case XRayWidgetMode.WHEN_ACTIVE => ScalaHintsSettings.xRayMode && !inLibrarySource && shouldHighlight
          case XRayWidgetMode.NEVER => false
        }
        e.getPresentation.setEnabledAndVisible(visible)
      }
    }
  }
}
