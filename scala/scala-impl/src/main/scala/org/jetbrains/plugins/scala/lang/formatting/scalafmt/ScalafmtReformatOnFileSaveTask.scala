package org.jetbrains.plugins.scala.lang.formatting.scalafmt

import consulo.dataContext.DataContext
import consulo.ui.ex.action.event.AnActionListener
import consulo.ui.ex.action.{ActionManager, AnAction, AnActionEvent, IdeActions}
import consulo.codeEditor.Editor
import consulo.undoRedo.CommandProcessor.{getInstance => CommandProcessor}
import consulo.document.Document
import consulo.document.FileDocumentManager
import consulo.fileEditor.{FileEditorManager, TextEditor}
import consulo.project.Project
import consulo.module.content.ProjectRootManager
import consulo.application.util.registry.Registry
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.{PsiDocumentManager, PsiFile}
import org.jetbrains.plugins.scala.extensions.{IterableOnceExt, ObjectExt, inReadAction}
import org.jetbrains.plugins.scala.lang.formatting.scalafmt.processors.ScalaFmtPreFormatProcessor
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.sbt.api.language.SbtFile

final class ScalafmtReformatOnFileSaveTask extends AnActionListener {

  override def afterActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent): Unit = {
    val project = event.getData(Project.KEY)
    if (project == null || project.isDisposed)
      return

    // upstream: matched on SaveDocumentAction/SaveAllAction classes (internal in Consulo)
    ActionManager.getInstance.getId(action) match {
      case "SaveDocument" => //no shortcut by default
        val editor = event.getData(Editor.KEY)
        if (editor != null) {
          reformatIfNeededInUndoTransparentAction(Seq(editor.getDocument))(using project)
        }
      case IdeActions.ACTION_SAVEALL => // Ctrl/Cmd + S (by default)
        //NOTE: for now we reformat only the selected editor, because it was how it worked before
        //But actually the "Save All" action saves all documents.
        //Maybe it makes sense to reformat all open editors? Maybe add an extra setting for that?
        //Anyway, it's not for the 2022.2 release, maybe it's ok to have it later
        val reformatOnlySelectedEditor = true
        val editors =
          if (reformatOnlySelectedEditor) Option(FileEditorManager.getInstance(project).getSelectedEditor).toSeq
          else FileEditorManager.getInstance(project).getAllEditors.toSeq

        val documents = editors.filterByType[TextEditor].map(_.getEditor.getDocument)
        reformatIfNeededInUndoTransparentAction(documents)(using project)
      case _ =>
    }
  }

  // for now 'reformat on file save' is only implemented for scalafmt formatter
  private def reformatIfNeededInUndoTransparentAction(documents: Seq[Document])(implicit project: Project): Unit = {
    if (project.isDisposed || !isScalafmtSaveOnfileEnabled(project))
      return

    val fileDocumentManager = FileDocumentManager.getInstance

    val currentTimeMs = System.currentTimeMillis()

    //debouncing: don't let "format on save" action be invoked too frequently
    val debouncingPassed = currentTimeMs > ScalafmtReformatOnFileSaveTask.LastFormatOnSaveTimeMs + ScalafmtReformatOnFileSaveTask.MinTimeBetweenFormatOnSaveActionMs
    if (debouncingPassed) {
      CommandProcessor.runUndoTransparentAction { () =>
        documents.foreach { document =>
          reformat(document, fileDocumentManager)
        }
      }
      ScalafmtReformatOnFileSaveTask.LastFormatOnSaveTimeMs = currentTimeMs
    }
  }

  private def reformat(document: Document, fileDocumentManager: FileDocumentManager)(implicit project: Project): Unit = {
    val vFile = fileDocumentManager.getFile(document)
    if (vFile == null)
      return

    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
    if (psiFile == null)
      return

    val isFileSupportedAndInSources = inReadAction {
      isFileSupported(psiFile) && isInProjectSources(psiFile, vFile)
    }
    if (isFileSupportedAndInSources) {
      ScalaFmtPreFormatProcessor.formatWithoutCommit(psiFile, document, respectProjectMatcher = true)
    }
  }

  private def isScalafmtSaveOnfileEnabled(project: Project): Boolean = {
    val scalaSettings = ScalaCodeStyleSettings.getInstance(project)
    scalaSettings.SCALAFMT_REFORMAT_ON_FILES_SAVE && scalaSettings.USE_SCALAFMT_FORMATTER()
  }

  private def isInProjectSources(psiFile: PsiFile, vFile: VirtualFile)(implicit project: Project): Boolean = {
    val fileIndex = ProjectRootManager.getInstance(project).getFileIndex
    psiFile match {
      case _: SbtFile =>
        fileIndex.isInContent(vFile)
      case _ =>
        fileIndex.isInSourceContent(vFile)
    }
  }

  private def isFileSupported(file: PsiFile): Boolean =
    file.is[ScalaFile]
}

private object ScalafmtReformatOnFileSaveTask {

  /**
   * Timestamp (e.g., System.currentTimeMillis) of the last invocation "Save All" or "Save Document" action.
   * The primary purpose of it is to debounce the "reformat on save" action if the user invokes it too frequently.
   *
   * NOTE: I intentionally decided that it would be enough to have a global static variable for all documents in all projects.
   * This was done not to overcomplicate the architecture.
   * "Save" actions are not supposed to be invoked too frequently by user, so this should be enough for our needs.
   */
  private var LastFormatOnSaveTimeMs: Long = 0

  private def MinTimeBetweenFormatOnSaveActionMs: Int = {
    Registry.intValue("scalafmt.min.time.between.format.on.save.action.ms")
  }
}
