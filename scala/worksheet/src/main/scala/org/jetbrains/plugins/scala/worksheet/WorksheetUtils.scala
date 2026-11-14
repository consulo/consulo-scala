package org.jetbrains.plugins.scala.worksheet

import consulo.language.editor.scratch.ScratchUtil
import consulo.codeEditor.Editor
import consulo.fileEditor.{FileEditorManager, TextEditor}
import consulo.project.Project
import consulo.application.util.registry.Registry
import consulo.util.lang.StringUtil
import consulo.virtualFileSystem.VirtualFile
import consulo.ui.annotation.{RequiredUIAccess => RequiresEdt}
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.incremental.scala.utils.ScalaJDKIncompatibilityDetector
import org.jetbrains.plugins.scala.api.ScalaFileType
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

object WorksheetUtils {

  // to test restoring of compiler messages positions in original worksheet file in one test method
  @TestOnly val ContinueOnFirstFailure = "scala.worksheet.continue.repl.evaluation.on.first.expression.failure"
  private val ShowReplErrorsInEditor = "scala.worksheet.show.repl.errors.in.editor"
  private val ShowReplErrorsInEditorInInteractiveMode = "scala.worksheet.show.repl.errors.in.editor.in.interactive.mode"

  @TestOnly def continueWorksheetEvaluationOnExpressionFailure: Boolean = Registry.is(ContinueOnFirstFailure, false)
  @TestOnly def showReplErrorsInEditor: Boolean = Registry.is(ShowReplErrorsInEditor, false)
  @TestOnly def showReplErrorsInEditorInInteractiveMode: Boolean = Registry.is(ShowReplErrorsInEditorInInteractiveMode, false)

  def isWorksheetFile(project: Project, file: VirtualFile): Boolean = {
    WorksheetFileType.isMyFileType(file) ||
      isScratchWorksheet(project, file)
  }

  def isScratchWorksheet(project: Project, file: VirtualFile): Boolean = {
    ScratchUtil.isScratch(file) && {
      WorksheetFileType.isMyFileType(file) ||
        WorksheetUtils.treatScratchFileAsWorksheet(project) && ScalaFileType.INSTANCE.isMyFileType(file)
    }
  }

  private def treatScratchFileAsWorksheet(project: Project): Boolean =
    settings(project).isTreatScratchFilesAsWorksheet

  private def settings(project: Project) =
    ScalaProjectSettings.getInstance(project)

  @RequiresEdt
  def getSelectedTextEditor(project: Project, file: VirtualFile): Option[Editor] =
    FileEditorManager.getInstance(project).getSelectedEditor(file) match {
      case txtEditor: TextEditor if txtEditor.getEditor != null =>
        Option(txtEditor.getEditor)
      case _ =>
        None
    }

  /**
   *
   * @see [[org.jetbrains.jps.incremental.scala.utils.ScalaJDKIncompatibilityDetector.prependWithWarning]]
   */
  private[worksheet] def prependWithJdkCompatibilityWarning(text: String, project: Project): String = {
    // upstream: the JDK of the compile server, here the compiler runs in the IDE process
    val jdkFeature = Some(Runtime.version.feature)
    val prepended = ScalaJDKIncompatibilityDetector.prependWithWarning(text, jdkFeature)
    StringUtil.convertLineSeparators(prepended)
  }
}
