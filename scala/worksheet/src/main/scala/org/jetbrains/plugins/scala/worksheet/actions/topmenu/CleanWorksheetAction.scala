package org.jetbrains.plugins.scala.worksheet.actions.topmenu

import consulo.annotation.component.ActionImpl

import consulo.language.editor.DaemonCodeAnalyzer
import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.ex.action.{AnAction, AnActionEvent}
import consulo.application.ApplicationManager
import consulo.codeEditor.Editor
import consulo.codeEditor.EditorEx
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.PsiFile
import consulo.ui.annotation.{RequiredUIAccess => RequiresEdt}
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.editor.DocumentExt
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.worksheet.WorksheetBundle
import org.jetbrains.plugins.scala.worksheet.runconfiguration.WorksheetCache
import org.jetbrains.plugins.scala.worksheet.ui.WorksheetDiffSplitters
import org.jetbrains.plugins.scala.worksheet.ui.printers.WorksheetEditorPrinterFactory

import consulo.ui.image.Image

import javax.swing.DefaultBoundedRangeModel

@ActionImpl(id = "Scala.CleanWorksheet")
class CleanWorksheetAction extends AnAction(
  WorksheetBundle.message("clean.scala.worksheet.action.text"),
  WorksheetBundle.message("clean.scala.worksheet.action.description"),
  PlatformIconGroup.actionsGc()
) with TopComponentAction {

  override def genericText: String = WorksheetBundle.message("worksheet.clear.button")

  override def actionIcon: Image = PlatformIconGroup.actionsGc()

  override def actionPerformed(e: AnActionEvent): Unit = {
    for { (editor, psiFile) <- getCurrentScalaWorksheetEditorAndFile(e) } {
      CleanWorksheetAction.cleanAll(editor, psiFile)
    }
  }
}

object CleanWorksheetAction {

  @TestOnly
  def cleanAll(editor: Editor, psiFile: PsiFile): Unit = {
    val project = psiFile.getProject
    val viewer = WorksheetCache.getInstance(project).getViewer(editor)

    if (psiFile == null || viewer == null)
      return

    invokeLater {
      inWriteAction {
        resetScrollModel(viewer)
        WorksheetCache.getInstance(project).removePrinter(editor)
        cleanWorksheet(psiFile.getVirtualFile, viewer, project)

        if (!ApplicationManager.getApplication.isUnitTestMode) {
          WorksheetDiffSplitters.removeSplitter(editor)
        }

        editor.getSettings.setFoldingOutlineShown(true)
        editor.getContentComponent.requestFocus() //  properly repaints editor SCL-16073
        DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
      }
    }
  }

  @RequiresEdt
  def resetScrollModel(viewer: Editor): Unit = {
    viewer match {
      case viewerEx: EditorEx if viewerEx.isScrollPaneAvailable =>
        val scrollbar = viewerEx.getScrollPane.getVerticalScrollBar
        val commonModel = scrollbar.getModel
        val newModel = new DefaultBoundedRangeModel(
          commonModel.getValue, commonModel.getExtent, commonModel.getMinimum, commonModel.getMaximum
        )
        inWriteAction {
          scrollbar.setModel(newModel)
        }
      case _ =>
    }
  }

  @RequiresEdt
  def cleanWorksheet(file: VirtualFile, rightEditor: Editor, project: Project): Unit = {
    val rightDocument = rightEditor.getDocument

    WorksheetEditorPrinterFactory.deleteWorksheetEvaluation(file)

    if (rightDocument != null && !project.isDisposed) {
      inWriteAction {
        rightDocument.setText("")
        rightDocument.commit(project)
      }
    }
  }
}