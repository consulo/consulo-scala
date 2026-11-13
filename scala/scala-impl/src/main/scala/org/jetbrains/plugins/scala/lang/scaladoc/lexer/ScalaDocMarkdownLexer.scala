package org.jetbrains.plugins.scala.lang.scaladoc.lexer

import consulo.language.lexer.MergingLexerAdapter
import consulo.language.ast.TokenSet


/*final class ScalaDocMarkdownLexer extends MergingLexerAdapter(
  new ScalaDocAsteriskStripperLexer((new ScalaDocMarkdownFlavour).createInlinesLexer()),
  ScalaDocMarkdownLexer.TokensToMerge
)*/

final class ScalaDocMarkdownLexer extends MergingLexerAdapter(new _ScalaDocMarkdownLexer, ScalaDocMarkdownLexer.TokensToMerge)

object ScalaDocMarkdownLexer {
  private val TokensToMerge = TokenSet.EMPTY
}