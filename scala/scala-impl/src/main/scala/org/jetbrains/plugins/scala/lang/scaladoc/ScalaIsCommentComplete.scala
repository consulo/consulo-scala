package org.jetbrains.plugins.scala.lang.scaladoc

import consulo.language.parser.ParserDefinition
import consulo.language.editor.action.{CommentCompleteHandler, FileQuoteHandler, JavaLikeQuoteHandler, LanguageQuoteHandler, QuoteHandler}
import consulo.language.version.LanguageVersionUtil
import consulo.language.{CodeDocumentationAwareCommenter, Language}
import consulo.language.lexer.Lexer
import consulo.codeEditor.Editor
import consulo.language.ast.IElementType
import consulo.language.psi.{PsiComment, PsiElement, PsiErrorElement, PsiFile}
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.extensions.PsiFileExt

class ScalaIsCommentComplete extends CommentCompleteHandler {

  override def isApplicable(comment: PsiComment, commenter: CodeDocumentationAwareCommenter): Boolean =
    comment.getParent.getLanguage.isKindOf(ScalaLanguage.INSTANCE)

  //same code in com.intellij.codeInsight.editorActions.EnterHandler
  override def isCommentComplete(comment: PsiComment, commenter: CodeDocumentationAwareCommenter, editor: Editor): Boolean = {
    val commentText: String = comment.getText
    val docComment: Boolean = isDocComment(comment, commenter)
    val expectedCommentEnd: String = if (docComment) commenter.getDocumentationCommentSuffix else commenter.getBlockCommentSuffix
    if (!commentText.endsWith(expectedCommentEnd)) return false
    val containingFile: PsiFile = comment.getContainingFile
    val language: Language = comment.getParent.getLanguage
    val lexer: Lexer = ParserDefinition.forLanguage(language).createLexer(LanguageVersionUtil.findDefaultVersion(language))
    val commentPrefix: String = if (docComment) commenter.getDocumentationCommentPrefix else commenter.getBlockCommentPrefix
    lexer.start(commentText, if (commentPrefix eq null) 0 else commentPrefix.length, commentText.length)
    // upstream: TypedHandler.getQuoteHandler (internal in Consulo)
    val fileTypeHandler: QuoteHandler = Option[QuoteHandler](FileQuoteHandler.forFileType(containingFile.getFileType))
      .getOrElse(LanguageQuoteHandler.forLanguage(containingFile.getViewProvider.getBaseLanguage))
    val javaLikeQuoteHandler: JavaLikeQuoteHandler =
      fileTypeHandler match {
        case quoteHandler: JavaLikeQuoteHandler => quoteHandler
        case _ => null
      }
    while (true) {
      val tokenType: IElementType = lexer.getTokenType
      if (tokenType eq null) {
        return false
      }
      if (javaLikeQuoteHandler != null && javaLikeQuoteHandler.getStringTokenTypes != null &&
        javaLikeQuoteHandler.getStringTokenTypes.contains(tokenType)) {
        val text: String = commentText.substring(lexer.getTokenStart, lexer.getTokenEnd)
        val endOffset: Int = comment.getTextRange.getEndOffset
        if (text.endsWith(expectedCommentEnd) && endOffset < containingFile.getTextLength && containingFile.charSequence.charAt(endOffset) == '\n') {
          return true
        }
      }
      var continue = false
      if (lexer.getTokenEnd == commentText.length) {
        if (lexer.getTokenType eq commenter.getLineCommentTokenType) {
          lexer.start(commentText, lexer.getTokenStart + commenter.getLineCommentPrefix.length, commentText.length)
          lexer.advance()
          continue = true
        }
        else if (isInvalidPsi(comment)) {
          return false
        } else {
          return lexer.getTokenEnd - lexer.getTokenStart == 2 //difference from EnterHandler
        }
      }
      if (!continue && (tokenType == commenter.getDocumentationCommentTokenType ||
        tokenType == commenter.getBlockCommentTokenType)) {
        return false
      } else if (!continue) {
        lexer.advance()
      }
    }
    false
  }

  private def isDocComment(element: PsiElement, commenter: CodeDocumentationAwareCommenter): Boolean = {
    if (!element.isInstanceOf[PsiComment]) return false
    val comment: PsiComment = element.asInstanceOf[PsiComment]
    commenter.isDocumentationComment(comment)
  }

  private def isInvalidPsi(base: PsiElement): Boolean = {
    var current: PsiElement = base.getNextSibling
    while (current != null) {
      if (current.getTextLength != 0) {
        return current.isInstanceOf[PsiErrorElement]
      }
      current = current.getNextSibling
    }
    false
  }
}

object ScalaIsCommentComplete extends ScalaIsCommentComplete