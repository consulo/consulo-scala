package org.jetbrains.plugins.scala.textAnalysis.grazie

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.plain.PlainTextLanguage
import consulo.language.spellcheker.text.TextContent.{Exclusion, TextDomain}
import consulo.language.spellcheker.text.{TextContent, TextContentBuilder, TextExtractor}
import consulo.language.spellcheker.text.util.{HtmlUtils, PsiUtils}
import org.jetbrains.plugins.scala.api.ScalaLanguage
import consulo.language.psi.PsiElement
import consulo.language.impl.psi.LeafPsiElement
import consulo.language.ast.TokenSet
import consulo.language.psi.PsiUtilCore
import consulo.util.collection.ContainerUtil
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiNamedElementExt}
import org.jetbrains.plugins.scala.intelliLang.injection.ScalaInjectionInfosCollector
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.{ScDocComment, ScDocInlinedTag, ScDocResolvableCodeReference, ScDocTag}
import org.jetbrains.plugins.scala.textAnalysis.spellchecker.ScalaDocCommentTokenizer

import java.util
import scala.jdk.CollectionConverters.SeqHasAsJava

/**
 * @see java example [[consulo.java.impl.spellchecker.JavaTextExtractor]]
 */
@ExtensionImpl
final class ScalaTextExtractor extends TextExtractor:

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  private val ExcludedScalaDocElementTypes: TokenSet = TokenSet.create(
    ScalaDocTokenType.DOC_COMMENT_START,
    ScalaDocTokenType.DOC_COMMENT_END,
    ScalaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS,
    //Exception in `@throws Exception description`, myParam `@param myParam description`
    ScalaDocTokenType.DOC_TAG_VALUE_TOKEN,
    ScalaDocTokenType.DOC_INNER_CODE,
    ScalaDocTokenType.DOC_HTTP_LINK_VALUE,
  )

  /** @see [[consulo.language.spellcheker.text.TextContent.ExclusionKind.unknown]] */
  private val UnknownTokens = TokenSet.create(
    //link in `[[...]]`` (note, we still process description inside (e.g. `[[a.b.c description text]]`
    ScalaDocTokenType.DOC_LINK_TAG,
    ScalaDocTokenType.DOC_LINK_CLOSE_TAG,
    ScalaDocTokenType.DOC_HTTP_LINK_TAG,
    ScalaDocTokenType.DOC_LIST_ITEM_HEAD,
  )

  private val scaladocBuilder: TextContentBuilder =
    TextContentBuilder.FromPsi
      .withUnknown: e =>
        e.is[ScDocInlinedTag] || e.is[LeafPsiElement] && UnknownTokens.contains(PsiUtilCore.getElementType(e))
      .excluding: e =>
        val elementType = PsiUtilCore.getElementType(e)
        ExcludedScalaDocElementTypes.contains(elementType) ||
          e.is[LeafPsiElement] && ScalaDocTokenType.ALL_SCALADOC_SYNTAX_ELEMENTS.contains(elementType) ||
          e.is[ScDocResolvableCodeReference]
      .removingIndents(" \t")
      .removingLineSuffixes(" \t")

  private val scaladocBuilderWithoutTags = scaladocBuilder.excluding(_.is[ScDocTag])

  override def buildTextContents(root: PsiElement, allowedDomains: util.Set[TextContent.TextDomain]): util.List[TextContent] =
    // Handle ScalaDoc comments
    if (allowedDomains.contains(TextDomain.DOCUMENTATION))
      root match
        case _: ScDocComment =>
          return HtmlUtils.excludeHtml(scaladocBuilderWithoutTags.build(root, TextDomain.DOCUMENTATION))
        case tag: ScDocTag if !ScalaDocCommentTokenizer.ExcludedTags.contains(tag.name) =>
          return HtmlUtils.excludeHtml(scaladocBuilder.build(root, TextDomain.DOCUMENTATION))
        case _ =>

    // Handle line & block comments
    def isPlainCommentToken(e: PsiElement): Boolean =
      ScalaTokenTypes.PLAIN_COMMENTS_TOKEN_SET.contains(PsiUtilCore.getElementType(e))

    if (allowedDomains.contains(TextDomain.COMMENTS) && isPlainCommentToken(root))
      val roots = PsiUtils.getNotSoDistantSimilarSiblings(root, e => isPlainCommentToken(e))
      return ContainerUtil.createMaybeSingletonList(
        TextContent.joinWithWhitespace('\n', ContainerUtil.mapNotNull(roots, (c: PsiElement) => {
          TextContentBuilder.FromPsi.removingIndents(" \t*/").removingLineSuffixes(" \t").build(c, TextDomain.COMMENTS)
        })))

    // Handle string literals
    if (allowedDomains.contains(TextDomain.LITERALS))
      root match
        case string: ScStringLiteral =>
          val content = TextContentBuilder.FromPsi.build(string, TextDomain.LITERALS)

          //TODO: extract this body to a separate method
          if content != null then
            // NOTE: `content` has trimmed whitespace, so the text inside can be not equal to the original string inner content.
            // Thus, `offsetFromLiteralStart` might not be equal to content start in string literal.
            val offsetFromLiteralStart = content.getRangesInFile.get(0).getStartOffset - string.getTextOffset
            val contentText = content.toString

            // Reusing existing logic from language injection.
            // We need to pass some language just because API requires it. It doesn't matter which language we pass
            val injectionInfos = ScalaInjectionInfosCollector.collectInjectionInfos(Seq(string), PlainTextLanguage.INSTANCE, "", "")
            val contentRangesInLiteral = injectionInfos.ranges

            val excludedRanges = contentRangesInLiteral.zip(contentRangesInLiteral.tail).flatMap { case (prev, next) =>
              // We need to use `max` and `max`
              // because `TextContentBuilder.build` returns content with spaces trimmed from both sides
              val start = (prev.third.getEndOffset - offsetFromLiteralStart).max(0).min(contentText.length)
              val end = (next.third.getStartOffset - offsetFromLiteralStart).max(0).min(contentText.length)

              // Multiline string literals without margin can empty ranges for every blank line
              // We need to filter such empty ranges because `Exclusion` constructor will fail otherwise
              if (start == end) None else {
                val isInterpolationInjectionExclusion = contentText.lift(start).contains('$')
                // Treat ${} injection as "Unknown" in order grammar check uses it as a border at which a new analysis should be started.
                // In string `s"this is example"` we can reliably run the check and detect missing article "an",
                // But in string `s"this is $text example"` we can't do it because $text could inject the article
                val isUnknown = isInterpolationInjectionExclusion
                Some(new Exclusion(start, end, isUnknown))
              }
            } //end flatMap

            val contentWithExcludedRanges = content.excludeRanges(excludedRanges.asJava)
            val contentWithProcessedEscapes = TextContentEscapeUtils.replaceBackslashEscapes(contentWithExcludedRanges, string)
            return ContainerUtil.createMaybeSingletonList(contentWithProcessedEscapes)
          end if
        case _ =>
      end match
    end if

    util.List.of()
  end buildTextContents
end ScalaTextExtractor
