package org.jetbrains.plugins.scalaDirective.highlighter;

import consulo.language.parser.ParserDefinition;
import consulo.language.lexer.Lexer;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.ast.IElementType;
import org.jetbrains.plugins.scala.highlighter.DefaultHighlighter;
import org.jetbrains.plugins.scalaDirective.lang.lexer.ScalaDirectiveTokenTypes;
import org.jetbrains.plugins.scalaDirective.ScalaDirectiveLanguage;

import java.util.HashMap;
import java.util.Map;

public final class ScalaDirectiveSyntaxHighlighter extends SyntaxHighlighterBase {

    private static final Map<IElementType, TextAttributesKey> ATTRIBUTES;

    static {
        ATTRIBUTES = new HashMap<>();
        ATTRIBUTES.put(ScalaDirectiveTokenTypes.tDIRECTIVE_PREFIX, DefaultHighlighter.SCALA_DIRECTIVE_PREFIX);
        ATTRIBUTES.put(ScalaDirectiveTokenTypes.tDIRECTIVE_COMMAND, DefaultHighlighter.SCALA_DIRECTIVE_COMMAND);
        ATTRIBUTES.put(ScalaDirectiveTokenTypes.tDIRECTIVE_KEY, DefaultHighlighter.SCALA_DIRECTIVE_KEY);
        ATTRIBUTES.put(ScalaDirectiveTokenTypes.tDIRECTIVE_VALUE, DefaultHighlighter.SCALA_DIRECTIVE_VALUE);
    }

    public ScalaDirectiveSyntaxHighlighter() {
        super();
    }

    @Override
    public Lexer getHighlightingLexer() {
        return ParserDefinition.forLanguage(ScalaDirectiveLanguage.INSTANCE)
                .createLexer(null);
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(ATTRIBUTES.get(tokenType));
    }
}
