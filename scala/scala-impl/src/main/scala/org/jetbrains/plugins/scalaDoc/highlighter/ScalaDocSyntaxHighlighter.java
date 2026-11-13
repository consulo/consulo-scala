package org.jetbrains.plugins.scalaDoc.highlighter;

import com.intellij.lang.LanguageParserDefinitions;
import consulo.language.lexer.Lexer;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.ast.IElementType;
import org.jetbrains.plugins.scala.highlighter.DefaultHighlighter;
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType;
import org.jetbrains.plugins.scalaDoc.ScalaDocLanguage;

import java.util.Collections;
import java.util.Map;

public final class ScalaDocSyntaxHighlighter extends SyntaxHighlighterBase {

    private static final Map<IElementType, TextAttributesKey> ATTRIBUTES =
            Collections.singletonMap(ScalaDocTokenType.DOC_TAG_NAME, DefaultHighlighter.SCALA_DOC_TAG);

    public ScalaDocSyntaxHighlighter() {
        super();
    }

    @Override
    public Lexer getHighlightingLexer() {
        return LanguageParserDefinitions.INSTANCE
                .forLanguage(ScalaDocLanguage.INSTANCE)
                .createLexer(null);
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(ATTRIBUTES.get(tokenType));
    }
}
