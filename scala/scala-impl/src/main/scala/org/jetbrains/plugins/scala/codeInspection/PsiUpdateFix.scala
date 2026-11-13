package org.jetbrains.plugins.scala.codeInspection

import consulo.codeEditor.Editor
import consulo.document.util.TextRange
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement
import consulo.language.psi.{PsiDocumentManager, PsiElement, PsiFile}
import consulo.localize.LocalizeValue
import consulo.project.Project

/**
 * Consulo replacement of IntelliJ `PsiUpdateModCommandAction` / `PsiBasedModCommandAction`:
 * a regular quick fix (and intention) which updates the original PSI inside a write action.
 */
abstract class PsiUpdateFix[T <: PsiElement](element: T)
  extends LocalQuickFixAndIntentionActionOnPsiElement(element) {

  def getFamilyName: String

  override def getText: LocalizeValue = LocalizeValue.of(getFamilyName)

  protected def isElementApplicable(element: T, context: PsiUpdateContext): Boolean = true

  override def isAvailable(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement): Boolean =
    isElementApplicable(startElement.asInstanceOf[T], PsiUpdateContext(project, file, None))

  override final def invoke(project: Project, file: PsiFile, editor: Editor, startElement: PsiElement, endElement: PsiElement): Unit =
    invoke(PsiUpdateContext(project, file, Option(editor)), startElement.asInstanceOf[T])

  protected def invoke(context: PsiUpdateContext, element: T): Unit
}

/**
 * Same as [[PsiUpdateFix]], but for a pair of elements (end element may be absent if it was invalidated)
 */
abstract class PsiUpdateFixOnTwoPsiElements[T <: PsiElement, U <: PsiElement](
  val getFamilyName: String,
  startElement: T,
  endElement: U,
) extends LocalQuickFixAndIntentionActionOnPsiElement(startElement, endElement) {

  override def getText: LocalizeValue = LocalizeValue.of(getFamilyName)

  override final def invoke(project: Project, file: PsiFile, editor: Editor, startElement: PsiElement, endElement: PsiElement): Unit =
    doInvoke(PsiUpdateContext(project, file, Option(editor)), startElement.asInstanceOf[T], endElement.asInstanceOf[U])

  protected def doInvoke(context: PsiUpdateContext, startElement: T, endElement: U): Unit
}

/**
 * Replacement of IntelliJ `ActionContext` + `ModPsiUpdater` for [[PsiUpdateFix]]: editor is absent when the fix is
 * applied from an inspection.
 */
final case class PsiUpdateContext(project: Project, file: PsiFile, editor: Option[Editor]) {

  def offset: Int = editor.fold(-1)(_.getCaretModel.getOffset)

  def selection: TextRange = editor.fold(TextRange.EMPTY_RANGE) { e =>
    val model = e.getSelectionModel
    TextRange.create(model.getSelectionStart, model.getSelectionEnd)
  }

  def moveCaretTo(offset: Int): Unit = editor.foreach { e =>
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(e.getDocument)
    e.getCaretModel.moveToOffset(offset)
  }

  def select(range: TextRange): Unit = editor.foreach { e =>
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(e.getDocument)
    e.getSelectionModel.setSelection(range.getStartOffset, range.getEndOffset)
    e.getCaretModel.moveToOffset(range.getEndOffset)
  }
}
