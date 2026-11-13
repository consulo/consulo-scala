package org.jetbrains.plugins.scala.editor.enterHandler

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.action.EnterHandlerDelegate.Result
import consulo.language.editor.action.{EnterHandlerDelegate, EnterHandlerDelegateAdapter}
import consulo.dataContext.DataContext
import consulo.codeEditor.Editor
import consulo.document.util.TextRange
import consulo.language.psi.PsiFile
import com.intellij.java.language.psi.util.PsiUtil
import org.jetbrains.plugins.scala.editor.{DocumentExt, EditorExt}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, inWriteAction}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

@ExtensionImpl
class ScalaDocMarkdownEnterHandler extends EnterHandlerDelegateAdapter {
  override def postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): EnterHandlerDelegate.Result = {
    if (!file.is[ScalaFile] || !editor.inDocComment(editor.offset)) {
      return Result.Continue
    }

    val document = editor.getDocument
    val project = file.getProject
    document.commit(project)

    val scalaFile = file.asInstanceOf[ScalaFile]
    val caretModel = editor.getCaretModel
    if (caretModel.getCaretCount != 1) {
      return Result.Continue
    }
    val caretOffset = caretModel.getOffset

    val elementAtCaret = scalaFile.findElementAt(caretOffset)
    if (elementAtCaret == null || !PsiUtil.isInMarkdownDocComment(elementAtCaret)) {
      return Result.Continue
    }

    val currentLineNumber = document.getLineNumber(caretOffset)

    if (currentLineNumber <= 0) {
      return Result.Continue
    }

    def getLineText(line: Int): String = {
      val lineStart = document.getLineStartOffset(line)
      val lineEnd = document.getLineEndOffset(line)
      val lineRange = TextRange.create(lineStart, lineEnd)
      document.getText(lineRange)
    }

    val prevLine = getLineText(currentLineNumber - 1)
    val (currentLine, caretAtEnd) = {
      val lineStart = document.getLineStartOffset(currentLineNumber)
      val lineEnd = document.getLineEndOffset(currentLineNumber)
      val currentLine = document.getText(TextRange.create(lineStart, caretOffset))
      val caretAtEnd = caretOffset == lineEnd
      (currentLine, caretAtEnd)
    }
    val prevLineWithoutCurrentLinePrefix = prevLine.stripPrefix(currentLine)

    val quotePrefix = prevLineWithoutCurrentLinePrefix.takeWhile {
      case '>' | ' ' | '\t' => true
      case c => c.isWhitespace
    }

    inWriteAction {
      var inserted = 0
      val afterQuotePrefix = prevLineWithoutCurrentLinePrefix.substring(quotePrefix.length)

      val toInsert =
        if (!allowListInsertionRegex.matches(afterQuotePrefix)) {
          None
        } else if (afterQuotePrefix.startsWith("-")) {
          Some(if (caretAtEnd) "- " else "  ")
        } else if (afterQuotePrefix.startsWith("+")) {
          Some(if (caretAtEnd) "+ " else "  ")
        } else if (afterQuotePrefix.startsWith("*")) {
          Some(if (caretAtEnd) "* " else "  ")
        } else {
          for {
            m <- numberedListRegex.findPrefixMatchOf(afterQuotePrefix)
            num <- m.group(1).toIntOption
            if !caretAtEnd || num < Int.MaxValue
          } yield {
            val listSep = m.group(2)
            if (caretAtEnd) s"${num + 1}$listSep "
            else s"$num$listSep ".map(_ => ' ')
          }
        }

      for (toInsert <- toInsert) {
        document.insertString(caretOffset, toInsert)
        inserted += toInsert.length
      }

      if (inserted > 0 || !quotePrefix.forall(_.isWhitespace)) {
        document.insertString(caretOffset, quotePrefix)
        inserted += quotePrefix.length
      }

      if (inserted > 0) {
        caretModel.moveToOffset(caretOffset + inserted)
      }
    }

    Result.Continue
  }

  // make sure the line does contain something else than the list head and is not a header
  private val allowListInsertionRegex = raw"""(((\d+[.)])|[-+*])\s+\S.*)|\s+""".r
  private val numberedListRegex = raw"""(\d+)([.)])""".r
}
