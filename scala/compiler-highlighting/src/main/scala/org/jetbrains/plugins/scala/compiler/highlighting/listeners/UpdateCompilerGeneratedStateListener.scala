package org.jetbrains.plugins.scala.compiler.highlighting.listeners

import consulo.application.ReadAction
import consulo.component.ProcessCanceledException
import consulo.language.editor.rawHighlight.HighlightInfoType
import consulo.language.psi.{PsiFile, PsiManager}
import consulo.logging.Logger
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import org.jetbrains.jps.incremental.scala.shared.Client.{ClientMsg, PosInfo}
import org.jetbrains.jps.incremental.scala.shared.MessageKind
import org.jetbrains.jps.incremental.scala.shared.remote.SerializablePath
import org.jetbrains.plugins.scala.compiler.highlighting.core.ExternalHighlighting.RangeInfo
import org.jetbrains.plugins.scala.compiler.highlighting.core.{CompilerGeneratedState, CompilerGeneratedStateManager, CompilerMessageKinds, CompilerOptions, ExternalHighlighting, FileCompilerGeneratedState}
import org.jetbrains.plugins.scala.compiler.highlighting.services.BackgroundExecutorService.executeOnBackgroundThreadInNotDisposed
import org.jetbrains.plugins.scala.compiler.highlighting.services.{ExternalHighlightersService, ProjectProgressService}
import org.jetbrains.plugins.scala.compiler.highlighting.util.DocumentUtil
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettings
import org.jetbrains.plugins.scala.project.{ModuleExt, ProjectPsiFileExt}
import org.jetbrains.plugins.scala.settings.ScalaHighlightingMode
import org.jetbrains.plugins.scala.shared.compiler.CompilerEvent
import org.jetbrains.plugins.scala.shared.util.CompilationId

import java.nio.file.Path

/**
 * upstream: a CompilerEventListener topic listener, here called by
 * [[org.jetbrains.plugins.scala.compiler.highlighting.core.CompilerEventGeneratingClient]] directly. Paths are
 * always local (upstream: Eel paths of remote environments).
 */
private[highlighting] class UpdateCompilerGeneratedStateListener(project: Project) {
  private final val CompilerPluginTypePrefix = "<type>" // CompilerPlugin.TypePrefix

  private final val CompilerPluginTypeSuffix = "</type>" // CompilerPlugin.TypeSuffix

  private val Log: Logger = Logger.getInstance(classOf[UpdateCompilerGeneratedStateListener])

  /**
   * The text `file` was compiled from in `compilationId`.
   */
  private def textStampOf(compilationId: CompilationId, file: VirtualFile): Long =
    compilationId.documentVersions.getOrElse(DocumentUtil.canonicalPath(file), file.getModificationStamp)

  private def findVirtualFile(source: SerializablePath): Option[VirtualFile] =
    Path.of(SerializablePath.unsafePathAsString(source)).toVirtualFile

  def eventReceived(event: CompilerEvent): Unit = {
    val oldState = CompilerGeneratedStateManager.get(project)

    event match {
      case CompilerEvent.CompilationStarted(_, _, _, _) =>
        val newHighlightOnCompilationFinished = oldState.toHighlightingState.filesWithHighlightings
        val newState = oldState.copy(highlightOnCompilationFinished = newHighlightOnCompilationFinished)
        CompilerGeneratedStateManager.update(project, newState)
      case CompilerEvent.MessageEmitted(compilationId, _, _, ClientMsg(MessageKind.Info, text, Some(source), _, Some(begin), Some(end), _)) if text.startsWith(CompilerPluginTypePrefix) =>
        findVirtualFile(source).foreach { virtualFile =>
          val tpe = text.substring(CompilerPluginTypePrefix.length, text.indexOf(CompilerPluginTypeSuffix).ensuring(_ != -1))
          val fileState = FileCompilerGeneratedState(compilationId, textStampOf(compilationId, virtualFile), Set.empty, Map(((begin, end), tpe)))
          val newState = replaceOrAppendFileState(oldState, virtualFile, fileState)
          CompilerGeneratedStateManager.update(project, newState)
        }
      case CompilerEvent.MessageEmitted(compilationId, _, _, msg) =>
        for {
          text <- Option(msg.text)
          source <- msg.source
          virtualFile <- findVirtualFile(source)
        } {
          def calculateRangeInfo(startInfo: Option[PosInfo], endInfo: Option[PosInfo], debugTag: String): Option[RangeInfo] =
            for {
              startPos <- startInfo
              endPos <- endInfo if startPos != endPos
            } yield RangeInfo.Range(startPos, endPos, debugTag)

          def pointerOrProblemStart(msg: ClientMsg): Option[PosInfo] =
            msg.pointer.orElse(msg.problemStart)

          val highlightingType = kindToHighlightInfoType(msg.kind, text, virtualFile)
          val rangeInfo = (highlightingType match {
            case HighlightInfoType.WRONG_REF =>
              if (msg.pointer.isEmpty) {
                // Special handling of WRONG_REF in BSP projects.
                val pointer = msg.problemEnd.flatMap {
                  case PosInfo(line, column) if column > 0 => Some(PosInfo(line, column - 1))
                  case _ => None
                }
                pointer.map(RangeInfo.Pointer(_))
                  .orElse {
                    // fall back to the regular highlighting behaviour for WRONG_REF
                    calculateRangeInfo(pointerOrProblemStart(msg), msg.problemEnd, s"bsp wrong_ref fallback case, msg=$msg")
                  }
              } else {
                // Wrong reference errors are usually highlighted starting from the pointer provided by the compiler.
                // Empirically, this only highlights the name of the symbol which cannot be resolved.
                calculateRangeInfo(pointerOrProblemStart(msg), msg.problemEnd, s"wrong_ref case, msg=$msg")
              }
            case _ if ScalaHighlightingMode.useCompilerRanges =>
              // If the setting is checked, the full text range provided by the compiler is used.
              calculateRangeInfo(msg.problemStart, msg.problemEnd, s"use compiler ranges true case, msg=$msg")
            case _ =>
              // Otherwise, the range from the pointer to the end is used, matching the behaviour before
              // SCL-21339, SCL-21292 were implemented.
              calculateRangeInfo(pointerOrProblemStart(msg), msg.problemEnd, s"default case, msg=$msg")
          }).orElse(pointerOrProblemStart(msg).map(RangeInfo.Pointer(_)))
          val highlighting = ExternalHighlighting(
            highlightType = highlightingType,
            message = text,
            rangeInfo = rangeInfo,
            diagnostics = msg.diagnostics
          )
          val fileState = FileCompilerGeneratedState(compilationId, textStampOf(compilationId, virtualFile), Set(highlighting), Map.empty)
          val newState = replaceOrAppendFileState(oldState, virtualFile, fileState)

          CompilerGeneratedStateManager.update(project, newState)
        }
      case CompilerEvent.DiagnosticsCleared(compilationId, _, source) =>
        // The compiler compiled this file and found nothing wrong with it.
        findVirtualFile(source).foreach { virtualFile =>
          val cleanState = FileCompilerGeneratedState(compilationId, textStampOf(compilationId, virtualFile), Set.empty, Map.empty)
          val newState = oldState.copy(
            files = oldState.files.updated(virtualFile, cleanState),
            highlightOnCompilationFinished = oldState.highlightOnCompilationFinished + virtualFile
          )
          CompilerGeneratedStateManager.update(project, newState)
        }
      case CompilerEvent.ProgressEmitted(_, _, progress) =>
        ProjectProgressService(project).setCompilationProgress(progress)
      case CompilerEvent.CompilationFinished(compilationId, _, sources) =>
        val vFiles = for {
          source <- sources
          virtualFile <- findVirtualFile(source)
        } yield virtualFile
        val intermediateState = vFiles.foldLeft(oldState) { case (acc, file) =>
          val emptyState = FileCompilerGeneratedState(compilationId, textStampOf(compilationId, file), Set.empty, Map.empty)
          replaceOrAppendFileState(acc, file, emptyState)
        }
        val toHighlight = intermediateState.highlightOnCompilationFinished
        // Do not hold highlighting information for invalid virtual files, such as deleted ones.
        val newState = CompilerGeneratedState(
          files = intermediateState.files.filter(_._1.isValid),
          highlightOnCompilationFinished = Set.empty
        )

        CompilerGeneratedStateManager.update(project, newState)
        ProjectProgressService(project).resetCompilationProgress()

        try ExternalHighlightersService(project).updateWolf()
        catch {
          case _: ProcessCanceledException =>
        }

        if (toHighlight.nonEmpty) {
          executeOnBackgroundThreadInNotDisposed(project) {
            try ExternalHighlightersService(project).applyHighlightingState(toHighlight, compilationId)
            catch {
              // don't know what else we can do if compilation was cancelled at this stage
              // probably just don't show updated highlightings
              case _: ProcessCanceledException =>
              // ignore
            }
          }
        }
      case _ =>
    }
  }

  private def kindToHighlightInfoType(kind: MessageKind, text: String, virtualFile: VirtualFile): HighlightInfoType = {
    import org.jetbrains.plugins.scala.compiler.highlighting.core.HighlightInfoType as HIT
    val scalacOptions = scalacOptionsForFile(virtualFile)
    CompilerMessageKinds.highlightInfoType(
      kind = kind,
      text = text,
      fatalWarningsFlag = CompilerOptions.containsFatalWarnings(scalacOptions),
      unusedImportsFlag = CompilerOptions.containsUnusedImports(scalacOptions)
    ) match {
      case HIT.WrongRef => HighlightInfoType.WRONG_REF
      case HIT.Error => HighlightInfoType.ERROR
      // upstream: HighlightInfoType.UNUSED_SYMBOL (severity of the unused declaration inspection)
      case HIT.UnusedSymbol => HighlightInfoType.RAW_UNUSED_SYMBOL
      case HIT.Warning => HighlightInfoType.WARNING
      case HIT.WeakWarning => HighlightInfoType.WEAK_WARNING
      case HIT.Information => HighlightInfoType.INFORMATION
    }
  }

  private def scalacOptionsForFile(virtualFile: VirtualFile): Seq[String] = {
    val psiFileOpt = findPsiFile(virtualFile)
    val moduleOpt = psiFileOpt.flatMap(_.module)
    val compilerSettingsOpt = psiFileOpt.flatMap(ScalaCompilerSettings.forFile)

    val optionsOpt = for {
      settings <- compilerSettingsOpt
      module <- moduleOpt
    } yield settings.getOptionsAsStrings(module.hasScala3)

    optionsOpt.getOrElse(Seq.empty)
  }

  private def findPsiFile(virtualFile: VirtualFile): Option[PsiFile] =
    ReadAction
      .nonBlocking[Option[PsiFile]](() => Option(PsiManager.getInstance(project).findFile(virtualFile)))
      .inSmartMode(project)
      .expireWhen(() => project.isDisposed)
      .executeSynchronously()

  private def replaceOrAppendFileState(oldState: CompilerGeneratedState,
                                       file: VirtualFile,
                                       fileState: FileCompilerGeneratedState): CompilerGeneratedState = {
    val newFileState = oldState.files.get(file) match {
      case Some(oldFileState) if oldFileState.compilationId == fileState.compilationId =>
        oldFileState.withExtraHighlightings(fileState.highlightings).withExtraTypes(fileState.types)
      case _ =>
        fileState
    }
    val newFileStates = oldState.files.updated(file, newFileState)
    val newToHighlight = oldState.highlightOnCompilationFinished + file
    oldState.copy(files = newFileStates, highlightOnCompilationFinished = newToHighlight)
  }
}
