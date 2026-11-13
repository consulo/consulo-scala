package org.jetbrains.plugins.scala.highlighter

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.highlight.CodeBlockSupportHandler
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.{IterableOnceExt, Parent, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.ScBegin
import org.jetbrains.plugins.scala.lang.psi.api.base.ScEnd

import java.util

@ExtensionImpl
class ScalaBlockSupportHandler extends CodeBlockSupportHandler {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def getCodeBlockMarkerRanges(psiElement: PsiElement): util.List[TextRange] = psiElement match {
    case Parent(ScBegin(begin, Some(end))) if psiElement == begin.keyword =>
      util.Arrays.asList(begin.keyword.getTextRange, end.keyword.getTextRange)
    case Parent(ScEnd(Some(begin), end)) if psiElement == end.keyword  =>
      util.Arrays.asList(begin.keyword.getTextRange, end.keyword.getTextRange)
    case _ =>
      util.Collections.EMPTY_LIST.asInstanceOf[util.List[TextRange]]
  }

  override def getCodeBlockRange(psiElement: PsiElement): TextRange =
    psiElement.parentsInFile.findByType[ScBegin]
      .flatMap(begin => begin.end.map(end => TextRange.create(begin.keyword.startOffset, end.keyword.endOffset)))
      .getOrElse(TextRange.EMPTY_RANGE)
}
