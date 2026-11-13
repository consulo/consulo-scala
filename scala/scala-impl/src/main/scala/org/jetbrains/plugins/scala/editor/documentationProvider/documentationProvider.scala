package org.jetbrains.plugins.scala.editor

import consulo.colorScheme.{EditorColorsManager, TextAttributesKey}
import consulo.colorScheme.TextAttributes
import consulo.language.Language
import consulo.language.editor.highlight.SyntaxHighlighterFactory
import consulo.project.Project
import consulo.ui.color.ColorValue
import consulo.util.lang.StringUtil
import org.jetbrains.plugins.scala.highlighter.DefaultHighlighter

import java.awt.Font


package object documentationProvider {

  private[documentationProvider]
  implicit class HtmlBuilderWrapper(private val delegate: StringBuilder) extends AnyVal {
    def appendKeyword(word: String): StringBuilder = appendAs(word, DefaultHighlighter.KEYWORD)

    def appendAs(word: String, textAttributesKey: TextAttributesKey): StringBuilder = {
      val textAttributes = EditorColorsManager.getInstance.getGlobalScheme.getAttributes(textAttributesKey)
      appendStyledSpan(delegate.underlying, textAttributes, word)
      delegate
    }

    def appendAs(word: String, textAttributes: TextAttributes): StringBuilder = {
      appendStyledSpan(delegate.underlying, textAttributes, word)
      delegate
    }
  }

  //"\n" separator works as well as `<br>` tag because definition section is located inside `<pre>` tag (which preserves new lines)
  //however "\n" is more convenient to us in expected test data
  val NewLineSeparatorInDefinitionSection = "\n"

  private def toHtmlColor(color: ColorValue): String = {
    val rgb = color.toRGB
    f"#${rgb.getRed}%02x${rgb.getGreen}%02x${rgb.getBlue}%02x"
  }

  /**
   * upstream: `com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil.appendStyledSpan` (with saturation factor 1.0),
   * it has no Consulo counterpart (text effects are not rendered here)
   */
  private[editor] def appendStyledSpan(buffer: java.lang.StringBuilder, attributes: TextAttributes, value: String): java.lang.StringBuilder = {
    val style = new java.lang.StringBuilder
    def appendProperty(name: String, value: String): Unit = style.append(name).append(":").append(value).append(";")

    if (attributes != null) {
      Option(attributes.getForegroundColor).foreach(c => appendProperty("color", toHtmlColor(c)))
      Option(attributes.getBackgroundColor).foreach(c => appendProperty("background-color", toHtmlColor(c)))
      attributes.getFontType match {
        case Font.BOLD => appendProperty("font-weight", "bold")
        case Font.ITALIC => appendProperty("font-style", "italic")
        case _ =>
      }
    }

    buffer.append("<span style=\"").append(style).append("\">")
      .append(StringUtil.notNullize(value))
      .append("</span>")
  }

  /**
   * upstream: `com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledCodeBlock`,
   * it has no Consulo counterpart, the code is highlighted by the language lexer
   */
  private[editor] def appendStyledCodeBlock(buffer: java.lang.StringBuilder, project: Project, language: Language, code: CharSequence): java.lang.StringBuilder = {
    buffer.append("<pre><code>")
    val text = code.toString.strip
    val highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, null)
    if (highlighter == null)
      buffer.append(StringUtil.escapeXmlEntities(text))
    else {
      val scheme = EditorColorsManager.getInstance.getGlobalScheme
      val lexer = highlighter.getHighlightingLexer
      lexer.start(text)
      while (lexer.getTokenType != null) {
        val tokenText = StringUtil.escapeXmlEntities(text.substring(lexer.getTokenStart, lexer.getTokenEnd))
        val attributes = highlighter.getTokenHighlights(lexer.getTokenType)
          .map(scheme.getAttributes)
          .filter(_ != null)
          .foldLeft(new TextAttributes())(TextAttributes.merge)
        if (attributes.getForegroundColor == null && attributes.getBackgroundColor == null && attributes.getFontType == Font.PLAIN)
          buffer.append(tokenText)
        else
          appendStyledSpan(buffer, attributes, tokenText)
        lexer.advance()
      }
    }
    buffer.append("</code></pre>")
  }
}
