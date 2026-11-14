package org.jetbrains.plugins.scala.compiler.highlighting.services.core

import consulo.codeEditor.{Editor, EditorFactory}
import consulo.language.editor.completion.lookup.LookupManager
import consulo.language.editor.highlight.UpdateHighlightersUtil
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.plugins.scala.caches.ModTracker.anyScalaPsiChange
import org.jetbrains.plugins.scala.codeInsight.implicits.ImplicitHints
import org.jetbrains.plugins.scala.compiler.highlighting.services.ExternalHighlightersService.{AppliedHighlightings, HighlightInfoData, HighlightingData, ScalaCompilerPassId}
import org.jetbrains.plugins.scala.extensions.invokeLater
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.settings.{ScalaHighlightingMode, ScalaProjectSettings}

import java.util.Collections
import scala.jdk.CollectionConverters.*

/**
 * Applies highlighting data and diagnostics to the workspace interface.
 *
 * Responsible for interfacing with the editor subsystem to visually render highlights,
 * trigger semantic structural updates, and synchronize errors with project-wide problem tracking services.
 *
 * upstream: also repaints the error stripe panel (ErrorStripeUpdateManager is internal in Consulo, the markup model
 * repaints the stripe marks itself). The Wolf has no problem
 * sources in Consulo: the problems of the files reported here are replaced.
 */
private[highlighting] class ExternalHighlightingUpdater(project: Project) {

  /**
   * Applies each file's highlightings to its editor, skipping a file that has been edited since they were built: their
   * offsets were computed against the text it had then.
   */
  def applyHighlightingInfo(highlightInfoData: HighlightInfoData): AppliedHighlightings = {
    val infos = highlightInfoData.highlightingData
    val expressions = highlightInfoData.psiElements
    val settings = ScalaProjectSettings.getInstance(project)

    def shouldApplyHighlightings(editor: Editor) = {
      // If autocomplete is in progress, apply only types but not errors (see CompilerTypeRequestListener)
      !(settings.isCompilerHighlightingScala3 && settings.isUseCompilerTypes) || LookupManager.getActiveLookup(editor) == null
    }

    if (!project.isDisposed) {
      val (current, stale) = infos.partition(data => data.document.getModificationStamp == data.textStamp)
      val applied = current.collect {
        case HighlightingData(editor, document, _, virtualFile, highlightInfos, _) if shouldApplyHighlightings(editor) =>
          UpdateHighlightersUtil.setHighlightersToEditor(
            project,
            document, 0, document.getTextLength,
            highlightInfos.asJavaCollection,
            editor.getColorsScheme,
            ScalaCompilerPassId
          )
          virtualFile
      }.toSet

      if (expressions.nonEmpty) {
        // We change the type of the expression without changing the PSI, so we trigger the update manually (see ScalaPsiChangeListener)
        expressions.foreach { e =>
          ScalaPsiManager.instance(project).clearOnScalaElementChange(e)
        }
        // Update usages (see ScalaFileImpl.subtreeChanged, Search.Method.getUsages)
        anyScalaPsiChange.incModificationCount()
        // Also update hints (type hints, implicit hints, x-ray mode, etc.)
        ImplicitHints.updateInAllEditors()
      }

      AppliedHighlightings(applied, stale.map(_.virtualFile).toSet)
    } else {
      // The project is disposed, so nothing was applied to the editors.
      AppliedHighlightings.empty
    }
  }

  // upstream: clearProblemsFromExternalSource(file, source)
  def clearWolfProblems(file: VirtualFile): Unit =
    if (file ne null) WolfTheProblemSolver.getInstance(project).clearProblems(file)

  def eraseEditorHighlightings(editor: Editor): Unit = {
    val document = editor.getDocument
    UpdateHighlightersUtil.setHighlightersToEditor(
      project,
      document,
      0,
      document.getTextLength,
      Collections.emptyList(),
      editor.getColorsScheme,
      ScalaCompilerPassId
    )
  }

  def eraseAllHighlightings(): Unit = {
    for {
      editor <- EditorFactory.getInstance.getAllEditors
      editorProject <- Option(editor.getProject)
      if editorProject == project
    } invokeLater {
      if (!project.isDisposed) {
        eraseEditorHighlightings(editor)
      }
    }
    clearAllWolfProblems()
  }

  /** The files this updater reported to the Wolf, Consulo's Wolf has no problem sources to tell them apart */
  @volatile private var reportedToWolf: Set[VirtualFile] = Set.empty

  private def clearAllWolfProblems(): Unit = {
    val wolf = WolfTheProblemSolver.getInstance(project)
    reportedToWolf.foreach(file => if (file.isValid) wolf.clearProblems(file))
    reportedToWolf = Set.empty
  }

  // upstream: clearProblemsFromExternalSource of all files, then reportProblemsFromExternalSource(file, source)
  private[highlighting] def informWolf(errorFiles: Set[VirtualFile]): Unit = {
    if (!project.isDisposed && ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(project)) {
      val wolf = WolfTheProblemSolver.getInstance(project)
      (reportedToWolf -- errorFiles).foreach(file => if (file.isValid) wolf.clearProblems(file))
      errorFiles.foreach { file =>
        Option(wolf.convertToProblem(file, 0, 0, Array("Compilation error"))).foreach { problem =>
          wolf.reportProblems(file, Collections.singletonList(problem))
        }
      }
      reportedToWolf = errorFiles
    }
  }
}
