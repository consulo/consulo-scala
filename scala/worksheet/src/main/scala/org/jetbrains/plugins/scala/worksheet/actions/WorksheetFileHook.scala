package org.jetbrains.plugins.scala
package worksheet
package actions

import consulo.language.editor.DaemonCodeAnalyzer
import consulo.annotation.component.{ComponentScope, TopicImpl}
import consulo.ui.ex.action.{ActionToolbar, AnAction}
import consulo.ui.ModalityState
import consulo.application.ReadAction
import consulo.document.Document
import consulo.codeEditor.EditorEx
import consulo.fileEditor.event.{FileEditorManagerBeforeListener, FileEditorManagerEvent, FileEditorManagerListener}
import consulo.document.FileDocumentManager
import consulo.fileEditor.{FileEditorManager, TextEditor}
import consulo.project.event.DumbModeListener
import consulo.project.Project
import consulo.virtualFileSystem.VirtualFile
import consulo.language.psi.{PsiDocumentManager, PsiManager}
import consulo.ui.ex.awt.ClientProperty
import consulo.application.util.concurrent.AppExecutorUtil
import consulo.ui.annotation.{RequiredUIAccess => RequiresEdt}
import jakarta.inject.Inject
import consulo.util.collection.ContainerUtil
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration
import org.jetbrains.plugins.scala.worksheet.actions.repl.WorksheetReplRunAction
import org.jetbrains.plugins.scala.worksheet.actions.topmenu.StopWorksheetAction.StoppableProcess
import org.jetbrains.plugins.scala.worksheet.interactive.WorksheetAutoRunner
import org.jetbrains.plugins.scala.worksheet.runconfiguration.WorksheetCache
import org.jetbrains.plugins.scala.worksheet.settings.WorksheetFileSettings
import org.jetbrains.plugins.scala.worksheet.ui.printers.WorksheetEditorPrinterFactory
import org.jetbrains.plugins.scala.worksheet.ui.{WorksheetControlPanel, WorksheetDiffSplitters, WorksheetFoldGroup}

import java.{util => ju}
import scala.util.control.NonFatal

object WorksheetFileHook {

  private[actions] val file2panel = ContainerUtil.createWeakValueMap[VirtualFile, WorksheetControlPanel]

  private[actions] def getAndRemovePanel(file: VirtualFile): Option[WorksheetControlPanel] =
    Option(file2panel.remove(file))

  private[actions] def getPanel(file: VirtualFile): Option[WorksheetControlPanel] =
    Option(file2panel.get(file))

  private[actions] def getDocumentFrom(project: Project, file: VirtualFile): Option[Document] = {
    val fileOpt = Option(PsiManager.getInstance(project).findFile(file))
    fileOpt.flatMap { file =>
      //getCachedDocument can return `null` for example in some tests where a non-physical file is used
      Option(PsiDocumentManager.getInstance(project).getCachedDocument(file))
    }
  }

  @RequiresEdt()
  def disableRun(file: VirtualFile, exec: Option[StoppableProcess]): Unit =
    WorksheetFileHook.getPanel(file).foreach(_.disableRun(exec))

  @RequiresEdt()
  def enableRun(file: VirtualFile, hasErrors: Boolean): Unit =
    WorksheetFileHook.getPanel(file).foreach(_.enableRun(hasErrors))

  def updateStoppableProcess(file: VirtualFile, exec: Option[StoppableProcess]): Unit =
    invokeLater {
      WorksheetFileHook.getPanel(file).foreach(_.updateStoppableProcess(exec))
    }

  def isRunning(file: VirtualFile): Boolean =
    WorksheetFileHook.getPanel(file).exists(!_.isRunEnabled)

  def moduleUpdated(project: Project, virtualFile: VirtualFile): Unit = {
    WorksheetModuleUtil.ensureModuleAttached(project, virtualFile)
    restartFileAnalyzing(project, virtualFile)
  }

  private def restartFileAnalyzing(project: Project, virtualFile: VirtualFile): Unit = {
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    if (psiFile != null) {
      psiFile match {
        case scalaFile: ScalaFile =>
          scalaFile.incContextModificationStamp()
        case _ =>
      }
      ScalaPsiManager.instance(project).clearAllCachesAndWait()
      PsiManager.getInstance(project).dropPsiCaches()
      DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
    }
  }

  def profileUpdated(project: Project, virtualFile: VirtualFile): Unit = {
    /**
     * Currently we have only module/project-level caching for profiles
     * (see caching in [[org.jetbrains.plugins.scala.lang.resolve.processor.precedence.PrecedenceTypes.forModule]])
     * (see caching in [[org.jetbrains.plugins.scala.project.ModuleExt.scalaModuleSettings]])
     *
     * If project compiler profiles configuration is not changed it means that no changes were made in any module profile.
     * Worksheet profile "lives" out of scope of [[org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration]]
     * So to trigger correct file re-highlighting we need to invalidate some compiler-options-specific caches.
     */
    ScalaCompilerConfiguration.incModificationCount()
    restartFileAnalyzing(project, virtualFile)
  }
}

// upstream: WorksheetFileHook.WorksheetBeforeEditorOpenedOrClosedListener
@TopicImpl(ComponentScope.PROJECT)
final class WorksheetFileHookWorksheetBeforeEditorOpenedOrClosedListener @Inject()(project: Project) extends FileEditorManagerBeforeListener.Adapter {
  override def beforeFileClosed(source: FileEditorManager, file: VirtualFile): Unit = {
   if (!isPluggable(file)) return

   val selectedEditor = source.getSelectedTextEditor
   if (selectedEditor != null) {
     val cache = WorksheetCache.getInstance(project)
     cache.editorClosed(selectedEditor)
   }
  }

  private def isPluggable(file: VirtualFile): Boolean = file.isValid &&
    WorksheetUtils.isWorksheetFile(project, file)
}


// upstream: @ApiStatus.Internal
// upstream: @ApiStatus.Experimental
// upstream: WorksheetFileHook.WorksheetEditorListener
@TopicImpl(ComponentScope.PROJECT)
final class WorksheetFileHookWorksheetEditorListener @Inject()(project: Project) extends FileEditorManagerListener {

  private def isPluggable(file: VirtualFile): Boolean = file.isValid &&
    WorksheetUtils.isWorksheetFile(project, file)

  override def selectionChanged(event: FileEditorManagerEvent): Unit = {}

  override def fileClosed(source: FileEditorManager, file: VirtualFile): Unit = {
    if (!isPluggable(file)) return

    val project = source.getProject
    val document = WorksheetFileHook.getDocumentFrom(project, file)
    document.foreach(WorksheetAutoRunner.getInstance(project).removeListener(_))
  }

  override def fileOpened(source: FileEditorManager, file: VirtualFile): Unit = {
    if (!isPluggable(file)) return

    val project = source.getProject

    WorksheetFileSettings(project, file).ensureSettingsArePersisted()
    WorksheetModuleUtil.ensureModuleAttached(project, file)
    initWorksheetUiComponents(file)
    loadEvaluationResult(project, source, file)

    val document = WorksheetFileHook.getDocumentFrom(project, file)
    document.foreach(WorksheetAutoRunner.getInstance(project).addListener(_))
  }

  private def loadEvaluationResult(project: Project, source: FileEditorManager, file: VirtualFile): Unit =
    for {
      editor <- editorWithFile(source, file)
    } loadEvaluationResult(project, file, editor)

  private def editorWithFile(source: FileEditorManager, file: VirtualFile): Option[EditorEx] =
    Option(source.getSelectedEditor(file))
      .collect { case te: TextEditor => te.getEditor }
      .collect { case e: EditorEx => e }

  private def loadEvaluationResult(project: Project, file: VirtualFile, editor: EditorEx): Unit = {
    val evaluationResultOpt = WorksheetEditorPrinterFactory.loadWorksheetEvaluation(file)
    evaluationResultOpt.foreach {
      case (result, ratio) if result.nonEmpty =>
        val viewer = WorksheetEditorPrinterFactory.createViewer(editor)
        val document = viewer.getDocument

        val splitterOpt = WorksheetDiffSplitters.getSplitter(editor)

        inWriteAction {
          document.setText(result)
          PsiDocumentManager.getInstance(project).commitDocument(document)

          splitterOpt.foreach { splitter =>
            try {
              splitter.setProportion(ratio)
              val group = WorksheetFoldGroup.load(viewer, editor, project, splitter, file)
              WorksheetEditorPrinterFactory.synch(editor, viewer, Some(splitter), Some(group))
            } catch {
              case NonFatal(_) => //ignored; if we are trying to load code stored in "plain" mode while in REPL mode
            }
          }
        }
      case _ =>
    }
  }

  private def initWorksheetUiComponents(file: VirtualFile): Unit = {
    if (project.isDisposed) return

    val myFileEditorManager = FileEditorManager.getInstance(project)
    val editors = myFileEditorManager.getAllEditors(file)

    for (editor <- editors) {
      WorksheetFileHook.getAndRemovePanel(file).foreach { panel =>
        invokeLater {
          myFileEditorManager.removeTopComponent(editor, panel)
        }
      }

      val controlPanel = new WorksheetControlPanel(editor.getComponent)
      val actions: ju.List[AnAction] = ju.List.of(new WorksheetReplRunAction)
      ClientProperty.put(editor.getComponent, AnAction.ACTIONS_KEY, actions)
      WorksheetFileHook.file2panel.put(file, controlPanel)
      myFileEditorManager.addTopComponent(editor, controlPanel)
    }
  }
}


// upstream: WorksheetFileHook.WorksheetDumbModeListener
@TopicImpl(ComponentScope.PROJECT)
final class WorksheetFileHookWorksheetDumbModeListener @Inject()(project: Project) extends DumbModeListener {
  override def enteredDumbMode(): Unit = {}
  override def exitDumbMode(): Unit = {
    // This is a three part operation.
    //   1. The platform calls the 'exitDumbMode' method on the UI thread.
    //      We immediately use this opportunity to query the currently open text editor and get its document
    //      if any editor is currently open.
    //   2. We use the ReadAction.nonBlocking machinery to schedule a known slow operation on a background thread.
    //      Here we fetch the corresponding VirtualFile instance for the obtained document.
    //   3. Finally, the ReadAction.nonBlocking machinery automatically shifts the computation back to the UI thread
    //      where we do the UI modification work (which must be done on the UI thread anyways).

    val editor = FileEditorManager.getInstance(project).getSelectedTextEditor
    if (editor == null) {
      // There are no active editors. Do nothing.
      return
    }

    val document = editor.getDocument
    ReadAction
      .nonBlocking[VirtualFile](() => virtualFileForDocument(document))
      .finishOnUiThread(_ => ModalityState.nonModal(), initializeButtons)
      .inSmartMode(project)
      .submit(AppExecutorUtil.getAppExecutorService)
  }

  // upstream: @RequiresBackgroundThread
  @Nullable
  private def virtualFileForDocument(document: Document): VirtualFile =
    FileDocumentManager.getInstance().getFile(document)

  @RequiresEdt
  private def initializeButtons(@Nullable virtualFile: VirtualFile): Unit = {
    if (virtualFile eq null) return
    val panel = WorksheetFileHook.getPanel(virtualFile)
    panel.foreach(notifyButtons)
  }

  @RequiresEdt
  private def notifyButtons(panel: WorksheetControlPanel): Unit =
    panel.getComponents.foreach {
      case toolbar: ActionToolbar =>
        toolbar.addNotify()
      case _ =>
    }
}
