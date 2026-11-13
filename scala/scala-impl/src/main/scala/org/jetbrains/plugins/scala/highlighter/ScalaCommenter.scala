package org.jetbrains.plugins.scala.highlighter

import consulo.language.codeStyle.CodeStyle
import consulo.language.SelfManagingCommenter
import consulo.language.CodeDocumentationAwareCommenter
import consulo.document.Document
import consulo.document.util.TextRange
import consulo.language.ast.IElementType
import consulo.language.psi.{PsiComment, PsiFile}
import consulo.util.lang.CharArrayUtil
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.scaladoc.parser.ScalaDocElementTypes

class ScalaCommenter extends SelfManagingCommenter[ScalaCommenterDataHolder] with CodeDocumentationAwareCommenter  {

  private val ScalaDirectivePrefix = "//>"

  override def getLineCommentPrefix = "//"

  override def getBlockCommentPrefix = "/*"
  override def getBlockCommentSuffix = "*/"

  override def getCommentedBlockCommentPrefix = "/*"
  override def getCommentedBlockCommentSuffix = "*/"

  override def getDocumentationCommentPrefix = "/**"
  override def getDocumentationCommentLinePrefix = "*"
  override def getDocumentationCommentSuffix = "*/"

  override def getLineCommentTokenType: IElementType = ScalaTokenTypes.tLINE_COMMENT
  override def getBlockCommentTokenType: IElementType = ScalaTokenTypes.tBLOCK_COMMENT
  override def getDocumentationCommentTokenType: IElementType = ScalaDocElementTypes.SCALA_DOC_COMMENT

  override def isDocumentationComment(element: PsiComment): Boolean = {
    val prefix = getDocumentationCommentPrefix
    prefix != null && {
      val text = element.getText
      text.startsWith(prefix) && !isEmptyBlockComment(text)
    }
  }

  private def isEmptyBlockComment(text: String): Boolean =
    text == "/**/" ||
      text.equals("/**\n/") // wired case when enter is pressed here /**<CARET>/

  override def createLineCommentingState(startLine: Int, endLine: Int, document: Document, file: PsiFile): ScalaCommenterDataHolder =
    new ScalaCommenterDataHolder(file)

  override def createBlockCommentingState(selectionStart: Int, selectionEnd: Int, document: Document, file: PsiFile): ScalaCommenterDataHolder =
    new ScalaCommenterDataHolder(file)

  override def commentLine(line: Int, offset: Int, document: Document, data: ScalaCommenterDataHolder): Unit = {
    val settings = CodeStyle.getLanguageSettings(data.getPsiFile)
    val prefix = getLineCommentPrefix + (if (settings.LINE_COMMENT_ADD_SPACE) " " else "")
    document.insertString(offset, prefix)
  }

  override def uncommentLine(line: Int, offset: Int, document: Document, data: ScalaCommenterDataHolder): Unit = {
    val documentText = document.getCharsSequence

    val prefix = getLineCommentPrefix
    val endOffset = offset + prefix.length

    //when LINE_COMMENT_ADD_SPACE is enabled "uncomment" should still handle both "//comment" and "// comment"
    val settings = CodeStyle.getLanguageSettings(data.getPsiFile)
    val trailingSpaceShift = if (settings.LINE_COMMENT_ADD_SPACE && documentText.charAt(endOffset) == ' ') 1 else 0

    document.deleteString(offset, endOffset + trailingSpaceShift)

    deleteSpacesIfLineIsBlank(document, documentText, line)
  }

  /**
   * Delete whitespace on line if that's all that left after uncommenting
   *
   * @note copied from [[com.intellij.codeInsight.generation.CommentByLineCommentHandler.doUncommentLine]]<br>
   *       which is not applicable for Scala language after we inherited SelfManagingCommenter
   */
  private def deleteSpacesIfLineIsBlank(document: Document, documentText: CharSequence, line: Int): Unit = {
    val lineStartOffset = document.getLineStartOffset(line)
    val lineEndOffset = document.getLineEndOffset(line)
    if (CharArrayUtil.isEmptyOrSpaces(documentText, lineStartOffset, lineEndOffset)) {
      document.deleteString(lineStartOffset, lineEndOffset)
    }
  }

  override def isLineCommented(line: Int, offset: Int, document: Document, data: ScalaCommenterDataHolder): Boolean = {
    CharArrayUtil.regionMatches(document.getCharsSequence, offset, getLineCommentPrefix) &&
      !CharArrayUtil.regionMatches(document.getCharsSequence, offset, ScalaDirectivePrefix)
  }

  override def getCommentPrefix(line: Int, document: Document, data: ScalaCommenterDataHolder): String =
    getLineCommentPrefix

  override def getBlockCommentRange(selectionStart: Int, selectionEnd: Int, document: Document, data: ScalaCommenterDataHolder): TextRange = {
    null
  }

  override def getBlockCommentPrefix(selectionStart: Int, document: Document, data: ScalaCommenterDataHolder): String =
    getBlockCommentPrefix

  override def getBlockCommentSuffix(selectionEnd: Int, document: Document, data: ScalaCommenterDataHolder): String =
    getBlockCommentSuffix

  override def uncommentBlockComment(startOffset: Int, endOffset: Int, document: Document, data: ScalaCommenterDataHolder): Unit = {
    val text = document.getCharsSequence
    def isAloneInLine(start: Int, end: Int): Boolean = {
      start == 0 || text.charAt(start - 1) == '\n' &&
        end < text.length() && text.charAt(end) == '\n'
    }

    val startIsAlone = isAloneInLine(startOffset, startOffset + getBlockCommentPrefix.length)
    val endIsAlone = isAloneInLine(endOffset - getBlockCommentSuffix.length, endOffset)

    val (prefix, suffix) =
      if (startIsAlone && endIsAlone)
        (getBlockCommentPrefix + "\n", getBlockCommentSuffix + "\n")
      else
        (getBlockCommentPrefix, getBlockCommentSuffix)

    // upstream: SelfManagingCommenterUtil.uncommentBlockComment
    document.deleteString(endOffset - suffix.length, endOffset)
    document.deleteString(startOffset, startOffset + prefix.length)
  }

  override def insertBlockComment(startOffset: Int, endOffset: Int, document: Document, data: ScalaCommenterDataHolder): TextRange = {
    val text = document.getCharsSequence
    def isAtStartOfLine(offset: Int): Boolean = {
      offset == 0 || text.charAt(offset - 1) == '\n'
    }

    val (prefix, suffix) =
      if (isAtStartOfLine(startOffset) && isAtStartOfLine(endOffset))
        (getBlockCommentPrefix + "\n", getBlockCommentSuffix + "\n")
      else
        (getBlockCommentPrefix, getBlockCommentSuffix)

    // upstream: SelfManagingCommenterUtil.insertBlockComment
    document.insertString(startOffset, prefix)
    document.insertString(endOffset + prefix.length, suffix)
    new TextRange(startOffset, endOffset + prefix.length + suffix.length)
  }
}

object ScalaCommenter extends ScalaCommenter
