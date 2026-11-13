package org.jetbrains.plugins.scala.editor.enterHandler

import consulo.annotation.component.ExtensionImpl

import consulo.codeEditor.Editor
import consulo.codeEditor.action.EditorActionHandler
import consulo.dataContext.DataContext
import consulo.document.Document
import consulo.document.util.TextRange
import consulo.language.ast.{IElementType, TokenType}
import consulo.language.codeStyle.CodeStyleManager
import consulo.language.editor.CodeInsightSettings
import consulo.language.editor.action.EnterHandlerDelegate.Result
import consulo.language.editor.action.{BraceMatchingUtil, EnterHandlerDelegateAdapter}
import consulo.language.psi.{PsiDocumentManager, PsiElement, PsiFile, PsiUtilCore}
import consulo.language.util.IncorrectOperationException
import consulo.logging.Logger
import consulo.util.lang.{CharArrayUtil, Pair, StringUtil}
import consulo.util.lang.ref.SimpleReference
import consulo.virtualFileSystem.fileType.FileType
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

/**
 * upstream: extended `com.intellij.codeInsight.editorActions.enter.EnterAfterUnmatchedBraceHandler`, which is an impl
 * class in Consulo, so its logic is inlined here (from the Consulo version of the class)
 */
@ExtensionImpl(order = "first")
class ScalaEnterAfterUnmatchedBraceHandler extends EnterHandlerDelegateAdapter {
  import ScalaEnterAfterUnmatchedBraceHandler._

  override def preprocessEnter(
    file: PsiFile,
    editor: Editor,
    caretOffsetRef: SimpleReference[Integer],
    caretAdvance: SimpleReference[Integer],
    dataContext: DataContext,
    originalHandler: EditorActionHandler
  ): Result = {
    val caretOffset: Int = caretOffsetRef.get
    if (!isApplicable(file, caretOffset))
      return Result.Continue

    val maxRBraceCount = getMaxRBraceCount(file, editor, caretOffset)
    if (maxRBraceCount > 0) {
      insertRBraces(file, editor,
        caretOffset,
        getRBraceOffset(file, editor, caretOffset),
        generateStringToInsert(editor, caretOffset, maxRBraceCount)
      )
      Result.DefaultForceIndent
    }
    else Result.Continue
  }

  def isApplicable(file: PsiFile, caretOffset: Int): Boolean =
    file.isInstanceOf[ScalaFile]

  protected def getMaxRBraceCount(file: PsiFile, editor: Editor, caretOffset: Int): Int =
    if (!CodeInsightSettings.getInstance.INSERT_BRACE_ON_ENTER) 0
    else Math.max(0, getUnmatchedLBracesNumberBefore(editor, caretOffset, file.getFileType))

  protected def generateStringToInsert(editor: Editor, caretOffset: Int, maxRBraceCount: Int): String = {
    val text = editor.getDocument.getCharsSequence
    var bracesToInsert = 0
    var i = caretOffset - 1
    var stop = false
    while (!stop && i >= 0 && bracesToInsert < maxRBraceCount) {
      val c = text.charAt(i)
      if (c == '{') bracesToInsert += 1
      else if (isStopChar(c)) stop = true
      i -= 1
    }
    StringUtil.repeatSymbol('}', Math.max(bracesToInsert, 1))
  }

  protected def isStopChar(c: Char): Boolean = " \n\t".indexOf(c) < 0

  // ATTENTION!
  // implementation of this method was copied from EnterAfterUnmatchedBraceHandler.java (version 191.6183.87)
  // list of changes:
  // 1) do not filter ignore question mark sign
  protected def getRBraceOffset(file: PsiFile, editor: Editor, caretOffset: Int): Int = {
    val text = editor.getDocument.getCharsSequence
    var offset = CharArrayUtil.shiftForward(text, caretOffset, " \t")
    val fileLength = text.length
    val blacklistChars = ")];,%<"
    if (offset < fileLength && blacklistChars.indexOf(text.charAt(offset)) < 0) {
      offset = calculateOffsetToInsertClosingBrace(file, text, offset).second
    }
    Math.min(offset, fileLength)
  }

  protected def insertRBraces(file: PsiFile, editor: Editor, caretOffset: Int, rBracesInsertOffset: Int, generatedRBraces: String): Unit = {
    val document = editor.getDocument
    insertRBracesAtPosition(document, caretOffset, rBracesInsertOffset, generatedRBraces)
    formatCodeFragmentBetweenBraces(file, document, caretOffset, rBracesInsertOffset, generatedRBraces)
  }

  protected def insertRBracesAtPosition(document: Document, caretOffset: Int, rBracesInsertOffset: Int, generatedRBraces: String): Unit = {
    document.insertString(rBracesInsertOffset, "\n" + generatedRBraces)
    // We need to adjust indents of the text that will be moved, hence, need to insert preliminary line feed.
    document.insertString(caretOffset, "\n")
  }

  protected def formatCodeFragmentBetweenBraces(file: PsiFile, document: Document, caretOffset: Int, rBracesInsertOffset: Int, generatedRBraces: String): Unit = {
    val project = file.getProject
    val stamp = document.getModificationStamp
    var closingBraceIndentAdjusted = false
    try {
      PsiDocumentManager.getInstance(project).commitDocument(document)
      CodeStyleManager.getInstance(project).adjustLineIndent(file, new TextRange(caretOffset, rBracesInsertOffset + 2))
    } catch {
      case e: IncorrectOperationException => Log.error(e)
    } finally {
      closingBraceIndentAdjusted = stamp != document.getModificationStamp
      // do you remember that we insert the '\n'? here we take it back!
      document.deleteString(caretOffset, caretOffset + 1)
    }

    // There is a possible case that formatter was unable to adjust line indent for the closing brace (that is the case for plain text
    // document for example). Hence, we're trying to do the manually.
    if (!closingBraceIndentAdjusted) {
      val line = document.getLineNumber(rBracesInsertOffset)
      val start = document.getLineStartOffset(line)
      val end = document.getLineEndOffset(line)
      val text = document.getCharsSequence
      val buffer = new StringBuilder
      var i = start
      while (i < end && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
        buffer.append(text.charAt(i))
        i += 1
      }
      if (buffer.nonEmpty) {
        document.insertString(rBracesInsertOffset + 1, buffer.toString)
      }
    }
  }

  // ATTENTION!
  // implementation of this method was copied from EnterAfterUnmatchedBraceHandler.java (version 191.6183.87)
  // list of changes:
  // 1) using element.getTextRange.getStartOffset instead of element.getTextOffset
  //    see getTextOffset documentation, looks like this is a typo in Intellij platform, because
  //    getTextOffset can be greater than get getTextRange.getStartOffset and we will not
  //    add closing brace in the end of such element in such case
  protected def calculateOffsetToInsertClosingBrace(file: PsiFile, text: CharSequence, offset: Int): Pair[PsiElement, Integer] = {
    var element = PsiUtilCore.getElementAtOffset(file, offset)
    val node = element.getNode
    if (node != null && (node.getElementType eq TokenType.WHITE_SPACE))
      return Pair.create(null, CharArrayUtil.shiftForwardUntil(text, offset, "\n"))

    var parent = element.getParent
    var done = false
    while (parent != null && !done) {
      val parentNode = parent.getNode
      if (parentNode == null || parentNode.getStartOffset != offset)
        done = true
      else {
        element = parent
        parent = parent.getParent
      }
    }

    // CHANGE
    if (element.getTextRange.getStartOffset != offset) {
      Pair.create(null, CharArrayUtil.shiftForwardUntil(text, offset, "\n"))
    } else {
      Pair.create(element, calculateOffsetToInsertClosingBraceInsideElement(element))
    }
  }

  protected def calculateOffsetToInsertClosingBraceInsideElement(element: PsiElement): Int =
    element.getTextRange.getEndOffset
}

object ScalaEnterAfterUnmatchedBraceHandler {
  private val Log = Logger.getInstance(classOf[ScalaEnterAfterUnmatchedBraceHandler])

  private def getUnmatchedLBracesNumberBefore(editor: Editor, offset: Int, fileType: FileType): Int = {
    if (offset == 0) return -1
    val chars = editor.getDocument.getCharsSequence
    if (chars.charAt(offset - 1) != '{') return -1

    val highlighter = editor.getHighlighter
    var iterator = highlighter.createIterator(offset - 1)
    val braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator)

    if (!braceMatcher.isLBraceToken(iterator, chars, fileType) || !braceMatcher.isStructuralBrace(iterator, chars, fileType))
      return -1

    val language = iterator.getTokenType.asInstanceOf[IElementType].getLanguage

    iterator = highlighter.createIterator(0)
    var lBracesBeforeOffset = 0
    var lBracesAfterOffset = 0
    var rBracesBeforeOffset = 0
    var rBracesAfterOffset = 0
    while (!iterator.atEnd) {
      val tokenType = iterator.getTokenType.asInstanceOf[IElementType]
      if (tokenType.getLanguage.equals(language) && braceMatcher.isStructuralBrace(iterator, chars, fileType)) {
        val beforeOffset = iterator.getStart < offset
        if (braceMatcher.isLBraceToken(iterator, chars, fileType)) {
          if (beforeOffset) lBracesBeforeOffset += 1
          else lBracesAfterOffset += 1
        }
        else if (braceMatcher.isRBraceToken(iterator, chars, fileType)) {
          if (beforeOffset) rBracesBeforeOffset += 1
          else rBracesAfterOffset += 1
        }
      }
      iterator.advance()
    }

    lBracesBeforeOffset - rBracesBeforeOffset - (rBracesAfterOffset - lBracesAfterOffset)
  }
}
