package org.jetbrains.plugins.scala.editor.documentationProvider

import consulo.language.editor.documentation.PsiCommentInlineDocumentation
import consulo.document.util.TextRange
import consulo.language.editor.documentation.{InlineDocumentation, InlineDocumentationProvider}
import consulo.language.psi.PsiFile
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocComment

import java.util

//noinspection UnstableApiUsage
final class ScalaInlineDocumentationProvider extends InlineDocumentationProvider {
  // The platform's compatibility provider collects comments through ScalaDocumentationProvider.collectDocComments.
  override def inlineDocumentationItems(file: PsiFile): util.Collection[InlineDocumentation] =
    util.Collections.emptyList()

  override def findInlineDocumentation(file: PsiFile, textRange: TextRange): InlineDocumentation = file match {
    case _: ScalaFile =>
      val comment = PsiTreeUtil.getParentOfType(file.findElementAt(textRange.getStartOffset), classOf[ScDocComment], false)
      if (comment != null && comment.getTextRange == textRange)
        new PsiCommentInlineDocumentation(comment)
      else
        null
    case _ => null
  }
}
