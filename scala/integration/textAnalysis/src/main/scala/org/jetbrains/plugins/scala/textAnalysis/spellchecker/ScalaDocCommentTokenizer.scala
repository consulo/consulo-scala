package org.jetbrains.plugins.scala.textAnalysis.spellchecker

import com.intellij.java.language.psi.javadoc.{PsiDocComment, PsiDocTag}
import consulo.language.spellcheker.tokenizer.splitter.CommentTokenSplitter
import consulo.language.spellcheker.tokenizer.{TokenConsumer, Tokenizer}
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, PsiNamedElementExt}

/**
 * @see [[consulo.java.impl.spellchecker.DocCommentTokenizer]] for the Java version
 */
private class ScalaDocCommentTokenizer extends Tokenizer[PsiDocComment] {
  override def tokenize(comment: PsiDocComment, consumer: TokenConsumer): Unit = {
    val splitter = CommentTokenSplitter.getInstance
    comment.children.foreach {
      case tag: PsiDocTag =>
        if (!ScalaDocCommentTokenizer.ExcludedTags.contains(tag.name))
          for (data <- tag.getDataElements)
            consumer.consumeToken(data, splitter)
      case el =>
        consumer.consumeToken(el, splitter)
    }
  }
}

private[textAnalysis] object ScalaDocCommentTokenizer {
  val ExcludedTags: Set[String] = Set(
    "author",
    "see",
  )
}
