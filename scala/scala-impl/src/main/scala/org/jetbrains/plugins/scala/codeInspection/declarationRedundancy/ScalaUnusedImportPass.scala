package org.jetbrains.plugins.scala.codeInspection.declarationRedundancy

import consulo.language.editor.{AutoImportHelper, DaemonCodeAnalyzer}
import consulo.language.editor.highlight.TextEditorHighlightingPass
import consulo.language.editor.rawHighlight.HighlightInfo
import consulo.language.editor.impl.highlight.HighlightInfoProcessor
import consulo.language.editor.highlight.HighlightingLevelManager
import consulo.language.editor.intention.IntentionAction
import consulo.language.editor.annotation.HighlightSeverity
import consulo.undoRedo.ProjectUndoManager
import consulo.undoRedo.util.UndoUtil
import consulo.document.Document
import consulo.codeEditor.Editor
import consulo.application.progress.ProgressIndicator
import consulo.project.Project
import consulo.util.dataholder.Key
import consulo.language.psi.{PsiDocumentManager, PsiFile}
import java.util.function.Predicate
import org.jetbrains.plugins.scala.annotator.usageTracker.UsageTracker
import org.jetbrains.plugins.scala.caches.CachesUtil.fileModCount
import org.jetbrains.plugins.scala.codeInspection.declarationRedundancy.ScalaUnusedImportPass.scheduleOnTheFlyImportOptimizer
import org.jetbrains.plugins.scala.editor.importOptimizer.ScalaImportOptimizer
import org.jetbrains.plugins.scala.extensions.{PsiFileExt, invokeLater}
import org.jetbrains.plugins.scala.incremental
import org.jetbrains.plugins.scala.incremental.Highlighting.builtInHighlightingDisabledIn
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

import java.{util => ju}
import scala.jdk.CollectionConverters.SeqHasAsJava

class ScalaUnusedImportPass(override val file: PsiFile, editor: Editor, override val document: Document,
                            highlightInfoProcessor: HighlightInfoProcessor)
  // upstream: extends ProgressableTextEditorHighlightingPass (internal in Consulo)
  extends TextEditorHighlightingPass(
    file.getProject,
    document,
    /*runIntentionPassAfter*/ false
  ) with ScalaUnusedImportPassBase {

  override protected def getFixes: List[IntentionAction] = List(new ScalaOptimizeImportsFix, new ScalaEnableOptimizeImportsOnTheFlyFix)

  private var myHighlights: ju.List[HighlightInfo] = scala.compiletime.uninitialized
  private var myOptimizeImportsRunnable: Option[Runnable] = None

  override def doCollectInformation(progress: ProgressIndicator): Unit = file match {
    case _ if builtInHighlightingDisabledIn(myProject) || incremental.Highlighting.enabledIn(myProject) => myHighlights = ju.Collections.emptyList()
    case _ if HighlightingLevelManager.getInstance(file.getProject).shouldInspect(file) =>
      file.findScalaLikeFile match {
        case Some(scalaFile: ScalaFile) =>
          val unusedImports = UsageTracker.getUnusedImports(scalaFile)
          val highlightInfos = collectHighlightings(unusedImports)
          myHighlights = highlightInfos.toList.asJava

          if (ScalaApplicationSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) {
            myOptimizeImportsRunnable = Some(new ScalaImportOptimizer(isOnTheFly = true).processFile(scalaFile, progress))
          }
        case _=>
      }
    case _: ScalaFile => myHighlights = ju.Collections.emptyList()
    case _ =>
  }

  override def getInfos: ju.List[HighlightInfo] =
    if (myHighlights == null) ju.Collections.emptyList() else myHighlights

  override def doApplyInformationToEditor(): Unit = {
    if (myHighlights == null) return

    implicit val project: Project = file.getProject
    highlightAll(myHighlights)
    ScalaUnusedImportPass.markFileUpToDate(file)

    if (editor != null && !myHighlights.isEmpty)
      myOptimizeImportsRunnable.foreach(scheduleOnTheFlyImportOptimizer(file, _))
  }
}

object ScalaUnusedImportPass {
  private val SCALA_LAST_POST_PASS_TIMESTAMP = Key.create[java.lang.Long]("SCALA_LAST_POST_PASS_TIMESTAMP")

  def scheduleOnTheFlyImportOptimizer(file: PsiFile, runnable: Runnable): Unit = {
    val project: Project = file.getProject
    val document: Document = PsiDocumentManager.getInstance(project).getDocument(file)
    if (document != null &&
        ScalaApplicationSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY &&
        file.isWritable &&
        ScalaUnusedImportPass.timeToOptimizeImports(file)
    ) {
      val stamp: Long = document.getModificationStamp

      invokeLater {
        if (!project.isDisposed &&
          document.getModificationStamp == stamp &&
          !ProjectUndoManager.getInstance(project).isUndoInProgress &&
          !ProjectUndoManager.getInstance(project).isRedoInProgress
        ) {
          PsiDocumentManager.getInstance(project).commitAllDocuments()
          UndoUtil.writeInRunUndoTransparentAction(runnable)
        }
      }
    }
  }

  private[codeInspection] def isUpToDate(file: PsiFile) =
    file.getUserData(SCALA_LAST_POST_PASS_TIMESTAMP) match {
      case null => false
      case lastStamp => lastStamp == fileModCount(file)
    }

  private def markFileUpToDate(file: PsiFile): Unit =
    file.putUserData(SCALA_LAST_POST_PASS_TIMESTAMP, java.lang.Long.valueOf(fileModCount(file)))

  private def timeToOptimizeImports(file: PsiFile): Boolean =
    ScalaApplicationSettings.getInstance.OPTIMIZE_IMPORTS_ON_THE_FLY && {
      val codeAnalyzer = DaemonCodeAnalyzer.getInstance(file.getProject)
      file match {
        case _: ScalaFile if codeAnalyzer.isHighlightingAvailable(file) && codeAnalyzer.isErrorAnalyzingFinished(file) =>
          !containsErrorsPreventingOptimize(file) && AutoImportHelper.getInstance(file.getProject).canChangeFileSilently(file)
        case _ => false
      }
    }

  private def containsErrorsPreventingOptimize(file: PsiFile): Boolean =
    PsiDocumentManager.getInstance(file.getProject).getDocument(file) match {
      case null => true
      case document: Document =>
        !DaemonCodeAnalyzer.processHighlights(
          document,
          file.getProject,
          HighlightSeverity.ERROR,
          0,
          document.getTextLength,
          ((_: HighlightInfo) => false) : Predicate[HighlightInfo] //todo: only unresolved ref issues?
        )

    }
}
