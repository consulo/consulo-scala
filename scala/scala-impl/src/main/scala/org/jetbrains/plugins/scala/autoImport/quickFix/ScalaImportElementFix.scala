package org.jetbrains.plugins.scala.autoImport.quickFix

import consulo.annotation.component.ComponentScope
import consulo.annotation.component.ServiceAPI
import consulo.annotation.component.ServiceImpl
import jakarta.inject.Singleton
import com.intellij.java.impl.codeInsight.JavaProjectCodeInsightSettings
import consulo.language.editor.hint.HintManager
import consulo.language.editor.inspection.PriorityAction
import consulo.language.editor.intention.HintAction
import consulo.application.{ApplicationManager, ReadAction}
import consulo.codeEditor.imaginary.ImaginaryEditor
import consulo.codeEditor.{Editor, LogicalPosition}
import consulo.project.Project
import consulo.component.util.ModificationTracker
import consulo.document.util.TextRange
import consulo.language.psi.{PsiElement, PsiFile}
import consulo.application.util.concurrent.AppExecutorUtil
import org.jetbrains.plugins.scala.autoImport.quickFix.Presentation.htmlWithBody
import org.jetbrains.plugins.scala.autoImport.quickFix.ScalaImportElementFix._
import org.jetbrains.plugins.scala.caches.BlockModificationTracker
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt, PsiFileExt, executeUndoTransparentAction, invokeLater, scheduleOnPooledThread}
import org.jetbrains.plugins.scala.lang.lexer.{ScalaKeywordTokenType, ScalaTokenTypes}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScGenericCall
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.api.isUnitTestMode

import java.awt.Point
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.{ExecutorService, TimeUnit}
import scala.annotation.nowarn

abstract class ScalaImportElementFix[Element <: ElementToImport](val place: PsiElement) extends HintAction with PriorityAction {

  private val modificationCount = currentModCount()

  private val computedElements: AtomicReference[Seq[Element]] = new AtomicReference()

  private val isComputationScheduled = new AtomicBoolean(false)

  if (isUnitTestMode) {
    val elementsToImport = findElementsToImport()
    computedElements.set(elementsToImport)
  }

  final def elements: Seq[Element] = Option(computedElements.get()).getOrElse(Seq.empty)

  protected def findElementsToImport(): Seq[Element]

  def createAddImportAction(editor: Editor): ScalaAddImportAction[?, ?]

  def shouldShowHint(): Boolean = !mayBeKeyword(place)

  def isAddUnambiguous: Boolean

  override def getPriority: PriorityAction.Priority =
    PriorityAction.Priority.TOP

  override def isAvailable(project: Project,
                           editor: Editor,
                           file: PsiFile): Boolean = {
    if (editor != null) {
      // upstream: SideEffectNotAllowedException (com.intellij.model.SideEffectGuard, no counterpart in Consulo) was caught here
      // to not schedule the computation inside an intention preview
      scheduleComputationOnce(editor)
    }

    isAvailable && file.hasScalaPsi
  }

  def isAvailable: Boolean = place.isValid && elements.nonEmpty && isUpToDate

  override def showHint(editor: Editor): Boolean = {
    if (elements.isEmpty || !shouldShowHint())
      false
    else if (fixSilently(editor))
      true
    else {
      invokeLater {
        if (!editor.isDisposed) showHintWithAction(editor)
      }
      true
    }

  }

  override def fixSilently(editor: Editor): Boolean = {
    if (elements.size == 1 && isAddUnambiguous && !editor.caretNear(place)) {
      executeUndoTransparentAction {
        createAddImportAction(editor).execute()
      }
      true
    }
    else false
  }

  override def invoke(project: Project,
                      editor: Editor,
                      file: PsiFile): Unit = executeUndoTransparentAction {
    if (isAvailable(project, editor, file)) {
      createAddImportAction(editor).execute()
    }
  }

  override def startInWriteAction: Boolean = true

  // upstream: generatePreview returned IntentionPreviewInfo.EMPTY (no intention preview in Consulo)

  protected def getHintRange: (Int, Int) = hintRange(place)

  private def hintRange(element: PsiElement): (Int, Int) = {
    element match {
      case ref: ScReference     => (ref.nameId.startOffset, ref.nameId.endOffset)
      case gcall: ScGenericCall => (hintRange(gcall.referencedExpr)._1, gcall.endOffset)
      case _                    => (element.startOffset, element.endOffset)
    }
  }

  private def showHintWithAction(editor: Editor): Unit = {
    val hintManager = HintManager.getInstance
    val (elementStart, elementEnd) = getHintRange

    if (place.isValid &&
      elements.nonEmpty &&
      !hintManager.hasShownHintsThatWillHideByOtherHint(true) &&
      editor.visibleRange.containsOffset(elementStart) &&
      elementEnd < editor.getDocument.getTextLength) {

      val hintText = ScalaBundle.message(
        "import.hint.text",
        elements.head.presentationBody,
        if (elements.length == 1) "" else ScalaBundle.message("import.multiple.choices")
      )

      hintManager.showQuestionHint(
        editor,
        htmlWithBody(hintText),
        elementStart,
        elementEnd,
        createAddImportAction(editor)
      )
    }
  }

  /**
   * Pre-computes elements to be potentially imported if the computation has not been scheduled yet.
   *
   * Could be useful in import fixer implementation to pre-compute elements
   * and filter applicable fixes.
   *
   * Must be called from the background thread as it is a potentially long-running task.
   */
  // upstream: @ApiStatus.Internal
  // upstream: @RequiresBackgroundThread
  final def preComputeElements(): Unit = if (place.isValid && isComputationScheduled.compareAndSet(false, true)) {
    val elementsToImport = findElementsToImport()
    computedElements.set(elementsToImport)
  }

  private def isUpToDate: Boolean = currentModCount() == modificationCount

  private def scheduleComputationOnce(editor: Editor): Unit = {
    invokeLater {
      //compute quickfix only if reference is visible in the current editor
      val shouldSchedule = place.isValid && isRelevant(editor) && editor.visibleRange.containsOffset(place.startOffset)

      if (shouldSchedule && isComputationScheduled.compareAndSet(false, true)) {
        val computationRunnable: Runnable = () => {
          val elementsToImport = findElementsToImport()
          computedElements.set(elementsToImport)
          scheduleShowHint(editor)
        }

        //TODO: consider not using ReadAction.nonBlocking as it's documentation suggests
        @nowarn("cat=deprecation")
        val task = ReadAction.nonBlocking(computationRunnable)
          .expireWhen(() => !isUpToDate || !isRelevant(editor))

        task.submit(boundedExecutor)
      }
    }
  }

  //if `showHint` is invoked when daemon is still running, hint may be hidden as a result of subsequent passes
  //(for example, ImplicitHintsPass hides all hints when it disposes old inlays)
  //introduce some delay as a workaround
  private def scheduleShowHint(editor: Editor): Unit =
    scheduleOnPooledThread(500, TimeUnit.MILLISECONDS) {
      invokeLater {
        if (isRelevant(editor) && isAvailable) {
          showHint(editor)
        }
      }
    }

  private def isRelevant(editor: Editor): Boolean =
    !editor.is[ImaginaryEditor] && !editor.isDisposed && editor.getContentComponent.isShowing

  private def currentModCount(): Long =
    if (place.isValid) BlockModificationTracker(place).getModificationCount
    else ModificationTracker.EVER_CHANGED.getModificationCount
}

private object ScalaImportElementFix {

  private def boundedExecutor: ExecutorService =
    ApplicationManager.getApplication.getInstance(classOf[ScalaImportElementFixExecutorHolder]).boundedTaskExecutor

  implicit class EditorEx(val editor: Editor) extends AnyVal {
    def caretNear(place: PsiElement): Boolean =
      place.getTextRange.grown(1).contains(editor.getCaretModel.getOffset)

    def visibleRange: TextRange = {
      // 1. There is no meaningful visible range for RemoteDev backend
      // 2. For non-local editor its visible area is unreliable
      //    (see IJPL-163871 Intentions sometimes don't appear in Remote Dev and Code With Me)
      //    (see SCL-19639 Code With Me: Auto-import quick-fix is offered only after some code editing on host)
      // upstream: returned the whole document for AppMode.isRemoteDevHost and non-local (ClientEditorManager) editors, no remote dev in Consulo

      val visibleRectangle = editor.getScrollingModel.getVisibleArea

      val startPosition = editor.xyToLogicalPosition(visibleRectangle.getLocation)
      val startOffset = editor.logicalPositionToOffset(startPosition)

      val endPosition = editor.xyToLogicalPosition(new Point(visibleRectangle.x + visibleRectangle.width, visibleRectangle.y + visibleRectangle.height))
      val endOffset = editor.logicalPositionToOffset(new LogicalPosition(endPosition.line + 1, 0))

      TextRange.create(
        startOffset,
        startOffset max endOffset
      )
    }

  }

  def isExcluded(qualifiedName: String, project: Project): Boolean =
    !isQualified(qualifiedName) ||
      JavaProjectCodeInsightSettings.getSettings(project).isExcluded(qualifiedName)

  private def isQualified(name: String) =
    name.indexOf('.') != -1

  private val softKeywords: Set[String] =
    ScalaTokenTypes.SOFT_KEYWORDS.getTypes.map(_.asInstanceOf[ScalaKeywordTokenType].keywordText).toSet

  private def mayBeKeyword(place: PsiElement): Boolean = place match {
    case ref: ScReference if ref.qualifier.isEmpty => softKeywords.contains(ref.refName)
    case _ => false
  }
}

// upstream: @Service
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
private final class ScalaImportElementFixExecutorHolder {
  val boundedTaskExecutor: ExecutorService =
    AppExecutorUtil.createBoundedApplicationPoolExecutor("ScalaImportElementFixExecutor", 2)
}
