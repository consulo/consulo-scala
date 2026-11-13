package org.jetbrains.plugins.scalaDirective.lang.parser

import consulo.language.version.LanguageVersion
import consulo.language.ast.ASTNode
import consulo.language.parser.PsiBuilder
import consulo.language.parser.PsiParser
import consulo.language.ast.IElementType
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scalaDirective.lang.lexer.ScalaDirectiveTokenTypes._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes.tLINE_COMMENT

class ScalaDirectiveParser extends PsiParser {

  /**
   * Parses a [[org.jetbrains.plugins.scalaDirective.psi.impl.ScDirectiveImpl]].
   *
   * The approach is to stop processing tokens after an unexpected or erroneous token is encountered.
   * This is tracked via <code>noErrorsOccurred</code>.
   */
  override def parse(root: IElementType, builder: PsiBuilder, languageVersion: LanguageVersion): ASTNode = {
    parseLight(root, builder)
    builder.getTreeBuilt
  }

  def parseLight(root: IElementType, builder: PsiBuilder): Unit = {

    var error = false

    /**
     * If the current token is one of the `expected` types, just advance the lexer.
     * If it isn't, insert an error element into the tree, set [[error]] to true and advance the lexer.
     */
    def processCurrentToken(builder: PsiBuilder, expected: IElementType*): Unit = {
      if (!error) {
        val currentTokenType = builder.getTokenType
        if (!expected.contains(currentTokenType)) {
          if (expected.contains(tDIRECTIVE_KEY)) builder.error("Scala directive key expected: option, dep, jar, etc.")
          else {
            builder.error("Unexpected token")
          }
          error = true
        }
      }

      builder.advanceLexer()
    }

    val rootMarker = builder.mark

    processCurrentToken(builder, tDIRECTIVE_PREFIX)
    processCurrentToken(builder, tDIRECTIVE_COMMAND)
    processCurrentToken(builder, tDIRECTIVE_KEY)

    while (builder.getTokenType != null) processCurrentToken(builder, tDIRECTIVE_VALUE, tDIRECTIVE_COMMA, tLINE_COMMENT)

    rootMarker.done(root)
  }
}