package org.jetbrains.plugins.scala.textAnalysis.spellchecker

import consulo.language.impl.psi.LeafPsiElement
import consulo.language.psi.{PsiComment, PsiElement}
import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.spellcheker.SpellcheckingStrategy
import consulo.language.spellcheker.SpellcheckingStrategy.EMPTY_TOKENIZER as emptyTokenizer
import consulo.language.spellcheker.tokenizer.splitter.PlainTextTokenSplitter
import consulo.language.spellcheker.tokenizer.{TokenConsumer, Tokenizer}
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.incremental.Highlighting.ElementHighlightingExt
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariable
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScModifierListOwner
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocComment

@ExtensionImpl
final class ScalaSpellcheckingStrategy extends SpellcheckingStrategy {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  private val myStringLiteralExpressionTokenizer: ScLiteralExpressionTokenizer = new ScLiteralExpressionTokenizer
  private val myDocCommentTokenizer: ScalaDocCommentTokenizer = new ScalaDocCommentTokenizer
  // upstream: TokenizerBase with an overridden consumeToken, Consulo's TokenizerBase has no such hook
  private val codeTokenizer: Tokenizer[PsiElement] = new Tokenizer[PsiElement] {
    override def tokenize(element: PsiElement, consumer: TokenConsumer): Unit =
      consumer.consumeToken(element, true, PlainTextTokenSplitter.getInstance())
  }

  override def getTokenizer(element: PsiElement): Tokenizer[? <: PsiElement] =
    // upstream: comments and literals are skipped when Grazie text level spellchecking is enabled
    element match {
      case _: ScStringLiteral => myStringLiteralExpressionTokenizer
      case _: ScDocComment => myDocCommentTokenizer
      case _: PsiComment => super.getTokenizer(element)
      case leaf: LeafPsiElement if leaf.getElementType == ScalaTokenTypes.tIDENTIFIER =>
        leaf.getParent match {
          case _: ScReference => emptyTokenizer
          case param: ScParameter =>
            param.owner match {
              case owner: ScModifierListOwner => getTokenizer(owner)
              case _ => codeTokenizer
            }
          case owner: ScModifierListOwner => getTokenizer(owner)
          case pattern: ScBindingPattern =>
            pattern.nameContext match {
              case valOrVar: ScValueOrVariable => getTokenizer(valOrVar)
              case _ => codeTokenizer
            }
          case _ => codeTokenizer
        }
      case _ => emptyTokenizer
    }

  private def getTokenizer(owner: ScModifierListOwner): Tokenizer[? <: PsiElement] =
    if owner.hasModifierPropertyScala("override") then emptyTokenizer else codeTokenizer

  override def isMyContext(element: PsiElement): Boolean = {
    val file = element.getContainingFile
    val project = if (file != null) file.getProject else element.getProject // Avoid tree walk-up

    element.isVisible(project, file)
  }
}
