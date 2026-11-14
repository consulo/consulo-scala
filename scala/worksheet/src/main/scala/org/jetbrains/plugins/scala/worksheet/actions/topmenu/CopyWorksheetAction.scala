package org.jetbrains.plugins.scala.worksheet.actions.topmenu

import consulo.annotation.component.ActionImpl

import consulo.platform.base.icon.PlatformIconGroup
import consulo.ui.ex.action.{AnAction, AnActionEvent}
import consulo.document.Document
import consulo.codeEditor.Editor
import consulo.ui.ex.CopyPasteManager
import consulo.util.lang.StringUtil
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.extensions.StringExt
import org.jetbrains.plugins.scala.worksheet.WorksheetBundle
import org.jetbrains.plugins.scala.worksheet.runconfiguration.WorksheetCache
import org.jetbrains.plugins.scala.worksheet.ui.WorksheetFoldGroup

import consulo.ui.image.Image

@ActionImpl(id = "Scala.CopyWorksheet")
class CopyWorksheetAction extends AnAction(
  WorksheetBundle.message("copy.scala.worksheet.action.text"),
  WorksheetBundle.message("copy.scala.worksheet.action.description"),
  PlatformIconGroup.actionsCopy()
) with TopComponentAction {

  override def genericText: String = WorksheetBundle.message("worksheet.copy.button")

  override def actionIcon: Image = PlatformIconGroup.actionsCopy()

  override def actionPerformed(e: AnActionEvent): Unit = {
    for {
      (editor, psiFile) <- getCurrentScalaWorksheetEditorAndFile(e)
      copiedText <- CopyWorksheetAction.prepareCopiableText(editor, psiFile)
    } {
      CopyPasteManager.getInstance.setText(copiedText)
    }
  }
}

object CopyWorksheetAction {

  private val COPY_BORDER = 80
  private val FILL_SYMBOL = " "

  def prepareCopiableText(editor: Editor, psiFile: PsiFile): Option[String] = {
    val viewer = WorksheetCache.getInstance(psiFile.getProject).getViewer(editor)
    Some(createMerged2(editor, viewer, psiFile).withNormalizedSeparator)
  }

  private def createMerged2(editor: Editor, viewer: Editor, psiFile: PsiFile): String = {
    val leftDocument = editor.getDocument
    if (viewer == null) return leftDocument.getText
    val rightDocument = viewer.getDocument
    if (rightDocument.getTextLength == 0) return leftDocument.getText

    val result = new StringBuilder
    val fullShift = StringUtil.repeat(" ", COPY_BORDER)
    val lineSeparator = "\n"

    val mappings = WorksheetFoldGroup.computeMappings(viewer, editor, psiFile.getVirtualFile)

    def getFromDoc(lineNumber: Int, document: Document): CharSequence =
      if (lineNumber >= document.getLineCount) "" else {
        val start = document.getLineStartOffset(lineNumber)
        val end   = document.getLineEndOffset(lineNumber)
        document.getImmutableCharSequence.subSequence(start, end)
      }

    def getLinesFrom(f: Int, t: Int, document: Document) =
      for (i <- f until t) yield getFromDoc(i, document)

    def getLines(doc: Document) = getLinesFrom(0, doc.getLineCount, doc)

    def append2Result(leftLines: Seq[CharSequence], rightLines: Seq[CharSequence]): Unit = {
      leftLines.zipAll(rightLines, "", fullShift).foreach {
        case (textLeft, textRight) =>
          result.append(StringUtil.trimTrailing(textLeft))

          if (textRight.length() > 0) {
            for (_ <- 1 to (COPY_BORDER - textLeft.length)) {
              result.append(FILL_SYMBOL)
            }
            result.append(" //")
            result.append(textRight)
          }

          result.append(lineSeparator)
      }
    }

    if (mappings.length < 2) {
      append2Result(getLines(leftDocument), getLines(rightDocument))
    } else {
      (mappings.tail :+ (leftDocument.getLineCount, rightDocument.getLineCount)).foldLeft(mappings.head) {
        case ((pl, pr), (l, r)) =>
          append2Result(getLinesFrom(pl, l, leftDocument), getLinesFrom(pr, r, rightDocument))
          (l, r)
      }
    }

    result.toString()
  }
}
