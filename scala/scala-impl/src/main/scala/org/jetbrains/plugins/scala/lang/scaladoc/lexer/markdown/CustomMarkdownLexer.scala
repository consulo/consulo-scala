package org.jetbrains.plugins.scala.lang.scaladoc.lexer.markdown

import consulo.language.ast.IElementType
import consulo.language.lexer.LexerBase

// upstream: extends org.intellij.markdown.lexer.GeneratedLexer, which Consulo's markdown engine doesn't have;
//           the engine's MarkdownLexer takes a platform lexer, see CustomMarkdownLexer.Adapter
trait CustomMarkdownLexer {
  def getState: Int = this.yystate()

  def yystate(): Int
  def yybegin(newState: Int): Unit
  def getTokenStart: Int
  def getTokenEnd: Int
  def reset(buffer: CharSequence, start: Int, end: Int, initialState: Int): Unit


  def advance(): IElementType = {
    yylex()
  }

  @throws[java.io.IOException]
  def yylex(): IElementType
}

object CustomMarkdownLexer {
  /**
   * Adapts the "advance returns the scanned token" contract of the generated lexer to the platform lexer contract,
   * the same way Consulo's JFlex skeleton does it for the engine's own lexers.
   */
  final class Adapter(lexer: CustomMarkdownLexer) extends LexerBase {
    // upstream: the Kotlin MarkdownLexer read `baseLexer.state` once on construction (`val state = baseLexer.state`),
    //           so the state reported to callers was always the initial one
    private val state = lexer.getState

    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var tokenType: IElementType = null
    private var located = false

    override def start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int): Unit = {
      this.buffer = buffer
      bufferEnd = endOffset
      lexer.reset(buffer, startOffset, endOffset, initialState)
      tokenType = null
      located = false
    }

    private def locateToken(): Unit =
      if (!located) {
        located = true
        tokenType = lexer.advance()
      }

    override def getState: Int = state

    override def getTokenType: IElementType = {
      locateToken()
      tokenType
    }

    override def getTokenStart: Int = {
      locateToken()
      lexer.getTokenStart
    }

    override def getTokenEnd: Int = {
      locateToken()
      lexer.getTokenEnd
    }

    override def advance(): Unit = {
      locateToken()
      located = false
    }

    override def getBufferSequence: CharSequence = buffer

    override def getBufferEnd: Int = bufferEnd
  }
}
