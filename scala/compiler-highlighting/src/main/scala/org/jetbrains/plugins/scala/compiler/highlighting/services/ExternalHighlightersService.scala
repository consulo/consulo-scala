package org.jetbrains.plugins.scala.compiler.highlighting.services

import consulo.annotation.access.RequiredReadAction
import consulo.annotation.component.{ComponentScope, ServiceAPI, ServiceImpl}
import consulo.application.progress.ProgressManager
import consulo.application.{Application, ReadAction}
import consulo.codeEditor.{Editor, EditorFactory}
import consulo.document.Document
import consulo.document.util.TextRange
import consulo.fileEditor.FileEditorManager
import consulo.language.editor.rawHighlight.{HighlightInfo, HighlightInfoType}
import consulo.language.psi.{PsiDocumentManager, PsiElement, PsiFile, PsiManager}
import consulo.logging.Logger
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import jakarta.inject.{Inject, Singleton}
import org.jetbrains.jps.incremental.scala.shared.Client.PosInfo
import org.jetbrains.plugins.scala.compiler.highlighting.core.CompilerGeneratedStateManager
import org.jetbrains.plugins.scala.compiler.highlighting.services.core.{ExternalHighlightingFixProvider, ExternalHighlightingUpdater, HighlightInfoFactory, HighlightingRangeCalculator}
import org.jetbrains.plugins.scala.editor.DocumentExt
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.impl.CompilerType
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode
import org.jetbrains.plugins.scala.shared.util.{CanonicalPath, CompilationId}

import java.util.concurrent.Callable
import scala.collection.immutable

/**
 * Orchestrates the application of external compiler diagnostics to the workspace.
 *
 * This service manages the asynchronous lifecycle, thread boundaries, and state mapping
 * required to retrieve raw compiler data, transform it into IDE-compatible structures,
 * and render those structures onto the active interface safely.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
final class ExternalHighlightersService @Inject()(project: Project) {

  import ExternalHighlightersService.{HighlightInfoData, HighlightingData}

  private val errorTypes: Set[HighlightInfoType] = Set(HighlightInfoType.ERROR, HighlightInfoType.WRONG_REF)

  private val rangeCalculator = new HighlightingRangeCalculator()
  private val fixProvider = new ExternalHighlightingFixProvider(project)
  private val highlightInfoFactory = new HighlightInfoFactory(project, rangeCalculator, fixProvider)
  private val updater = new ExternalHighlightingUpdater(project)

  /**
   * Applies the recorded diagnostics for `virtualFiles` to the editors showing them. The state is read when the work
   * runs, not when it is scheduled.
   *
   * A file is applied only if its recorded results were computed from its current text, checked when the
   * highlightings are built and again on the EDT. Otherwise, it is left as is: the editor keeps moving its
   * highlighters with the text until its own compilation replaces them.
   */
  def applyHighlightingState(virtualFiles: Set[VirtualFile], compilationId: CompilationId): Unit = {
    if (project.isDisposed) return

    val readActionCallable: Callable[HighlightInfoData] = { () =>
      val state = CompilerGeneratedStateManager.get(project).toHighlightingState
      val filteredVirtualFiles = filterFilesToHighlightBasedOnFileLevel(virtualFiles)
      val psiManager = PsiManager.getInstance(project)
      val shown = for {
        editor <- EditorFactory.getInstance().getAllEditors.toSeq if !project.isDisposed
        editorProject <- Option(editor.getProject) if editorProject == project
        document = editor.getDocument
        virtualFile <- document.virtualFile if filteredVirtualFiles.contains(virtualFile)
        psiFile <- Option(psiManager.findFile(virtualFile)) if ScalaHighlightingMode.isShowErrorsFromCompilerEnabled(psiFile)
      } yield (editor, document, virtualFile, psiFile)
      val (current, stale) = shown.partition { case (_, _, virtualFile, _) => state.describesCurrentText(virtualFile) }
      val data = current.map { case (editor, document, virtualFile, psiFile) =>
        val externalHighlights = state.externalHighlightings(virtualFile)
        val highlightInfos = highlightInfoFactory.calculateHighlightInfos(externalHighlights, document, psiFile)
        HighlightingData(editor, document, psiFile, virtualFile, highlightInfos, document.getModificationStamp)
      }

      var elements = Vector.empty[PsiElement]
      // Types only for the selected editor (could be extended to all open ones), and only on the compiled text: a
      // type goes to the expression with exactly the recorded range, which on other text is a different one.
      Option(FileEditorManager.getInstance(project).getSelectedTextEditor)
        .flatMap(editor => Option(editor.getVirtualFile))
        .filter(state.describesCurrentText)
        .foreach { file =>
          state.externalTypes(file).foreach { case ((begin, end), tpe) =>
            val psiFile = PsiManager.getInstance(project).findFile(file)
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)

            def toOffset(pos: PosInfo): Int = document.getLineStartOffset(pos.line - 1) + pos.column - 1

            val range = new TextRange(toOffset(begin), toOffset(end))
            psiFile
              .depthFirst(_.getTextRange.contains(range)) // Optimized iteration
              .filter(_.getTextRange == range)
              .find(e => e.is[ScExpression, ScStableCodeReference])
              .foreach { e =>
                // Skip if the same value already exists
                if (!CompilerType(e).contains(tpe)) {
                  // Doesn't require a write action
                  CompilerType(e) = Some(tpe)
                  elements :+= e
                }
              }
          }
        }
      HighlightInfoData(data, elements, stale.map { case (_, _, virtualFile, _) => virtualFile }.toSet)
    }

    ReadAction
      .nonBlocking(readActionCallable)
      .inSmartMode(project)
      .expireWhen(() => project.isDisposed)
      .coalesceBy(compilationId)
      .finishOnUiThread((_: Application) => Application.get().getNoneModalityState, (data: HighlightInfoData) => {
        updater.applyHighlightingInfo(data)
        ()
      })
      .submit(BackgroundExecutorService.instance(project).executor)
    ()
  }

  /**
   * Applies the diagnostics already recorded for `files` to whichever editors are showing them, compiling
   * nothing.
   *
   * Recorded diagnostics only reach an editor as a compilation finishes, so a file that had no editor open
   * at that moment carries results that were never displayed.
   *
   * A file whose recorded results were computed from a text other than the one it has now is left as it is, as
   * [[applyHighlightingState]] does for every file, and files that have become invalid meanwhile are dropped.
   *
   * @return how many recorded diagnostics the files had between them
   */
  def renderRecordedDiagnostics(files: Set[VirtualFile]): Int =
    if (files.isEmpty) 0
    else {
      val recorded = CompilerGeneratedStateManager.get(project).toHighlightingState
      applyHighlightingState(files, ExternalHighlightersService.renderOf())
      files.toSeq.map(recorded.externalHighlightings(_).size).sum
    }

  /**
   * Reports to the Wolf the files the recorded diagnostics have errors in, and clears the rest.
   *
   * Runs on the calling thread so that successive compilations reach the Wolf in order.
   */
  def updateWolf(): Unit = {
    val update: Callable[Unit] = { () =>
      val state = CompilerGeneratedStateManager.get(project).toHighlightingState
      updater.informWolf(filterFilesToHighlightBasedOnFileLevel(state.filesWithHighlightings(errorTypes)))
    }
    ReadAction
      .nonBlocking(update)
      .expireWhen(() => project.isDisposed)
      .executeSynchronously()
  }

  def eraseAllHighlightings(): Unit = {
    updater.eraseAllHighlightings()
  }

  def eraseHighlightings(editor: Editor): Unit = {
    updater.eraseEditorHighlightings(editor)
    updater.clearWolfProblems(editor.getVirtualFile)
  }

  def clearWolfProblems(file: VirtualFile): Unit =
    updater.clearWolfProblems(file)

  @RequiredReadAction
  private def filterFilesToHighlightBasedOnFileLevel(files: Set[VirtualFile]): Set[VirtualFile] = {
    val manager = PsiManager.getInstance(project)
    files.filter { vf =>
      ProgressManager.checkCanceled()
      if (vf.isValid) {
        val psiFile = manager.findFile(vf)
        if (psiFile ne null) ScalaHighlightingMode.shouldHighlightBasedOnFileLevel(psiFile, project) else false
      } else false
    }
  }
}

object ExternalHighlightersService {
  final val ScalaCompilerPassId = 979132998

  /**
   * Identifies one application of already-recorded diagnostics. Whether each file can take them is checked per file
   * when they are applied, so this carries no document versions. The timestamp is unique, which keeps the work from
   * coalescing with any other.
   */
  private def renderOf(): CompilationId =
    CompilationId(System.nanoTime(), immutable.HashMap.empty[CanonicalPath, Long])

  /** @param staleFiles the files left out because their recorded results describe another text */
  final case class HighlightInfoData(highlightingData: Seq[HighlightingData],
                                     psiElements: Seq[PsiElement],
                                     staleFiles: Set[VirtualFile])

  /**
   * What one application did: the files whose editors received their highlightings, and the files left as they
   * were because their results no longer describe their text.
   */
  final case class AppliedHighlightings(applied: Set[VirtualFile], stale: Set[VirtualFile])

  object AppliedHighlightings:
    val empty: AppliedHighlightings = AppliedHighlightings(Set.empty, Set.empty)

  /** @param textStamp the document's modification stamp `highlightInfos` were built against */
  final case class HighlightingData(editor: Editor,
                                    document: Document,
                                    psiFile: PsiFile,
                                    virtualFile: VirtualFile,
                                    highlightInfos: Set[HighlightInfo],
                                    textStamp: Long)

  final val Log: Logger = Logger.getInstance(classOf[ExternalHighlightersService])

  def apply(project: Project): ExternalHighlightersService =
    project.getInstance(classOf[ExternalHighlightersService])

  final case class TextRangeWithEndOfLine(textRange: TextRange, endOfLine: Boolean)
}
