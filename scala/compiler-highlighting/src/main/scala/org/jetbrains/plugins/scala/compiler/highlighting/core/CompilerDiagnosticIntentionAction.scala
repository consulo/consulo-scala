package org.jetbrains.plugins.scala.compiler.highlighting.core

import consulo.codeEditor.Editor
import consulo.document.util.TextRange
import consulo.document.{Document, RangeMarker}
import consulo.language.codeStyle.CodeStyleManager
import consulo.language.editor.intention.SyntheticIntentionAction
import consulo.language.psi.PsiFile
import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.plugins.scala.compiler.diagnostics.Action
import org.jetbrains.plugins.scala.editor.DocumentExt
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

import scala.jdk.CollectionConverters.*

// upstream: also generatePreview (no intention preview in Consulo)
private final class CompilerDiagnosticIntentionAction private (
  text: String,
  textEdits: Seq[(RangeMarker, String)]
) extends SyntheticIntentionAction {

  override def getText: LocalizeValue = LocalizeValue.of(text)

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = {
    !project.isDisposed && editor.getDocument.isWritable && file.isWritable && file.is[ScalaFile] &&
      textEdits.forall(_._1.isValid)
  }

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit = {
    val document = editor.getDocument
    applyDiagnostic(project, file, document, textEdits)
  }

  override def startInWriteAction(): Boolean = true

  private def applyDiagnostic(
    project: Project,
    file: PsiFile,
    document: Document,
    edits: Seq[(RangeMarker, String)]
  ): Unit = {
    if (edits.isEmpty) return

    edits.foreach { case (marker, text) =>
      val range = marker.getTextRange
      document.replaceString(range.getStartOffset, range.getEndOffset, text)
    }

    document.commit(project)
    val ranges = edits.map { case (marker, _) => growLeftAndRight(marker.getTextRange, document) }.asJava
    CodeStyleManager.getInstance(project).reformatText(file, ranges)

    edits.foreach { case (marker, _) => marker.dispose() }
  }

  private def growLeftAndRight(range: TextRange, document: Document): TextRange = {
    val text = document.getCharsSequence
    val length = text.length()

    var startOffset = range.getStartOffset
    while (startOffset > 0 && !text.charAt(startOffset - 1).isWhitespace) {
      startOffset -= 1
    }

    var endOffset = range.getEndOffset
    while (endOffset < length && !text.charAt(endOffset).isWhitespace) {
      endOffset += 1
    }

    TextRange.create(startOffset, endOffset)
  }
}

private[highlighting] object CompilerDiagnosticIntentionAction {
  def create(document: Document, action: Action): CompilerDiagnosticIntentionAction = {
    val text = action.title.capitalize
    val markers = action.edit.changes.map { te =>
      val start = document.getLineStartOffset(te.start.line - 1) + te.start.column - 1
      val end = document.getLineStartOffset(te.end.line - 1) + te.end.column - 1
      val marker = document.createRangeMarker(start, end)
      marker.setGreedyToRight(true)
      (marker, te.newText)
    }
    new CompilerDiagnosticIntentionAction(text, markers)
  }
}
