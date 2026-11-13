package org.jetbrains.plugins.scalaDoc.highlighter;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;
import org.jetbrains.plugins.scalaDoc.ScalaDocLanguage;

@ExtensionImpl
public final class ScalaDocSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {
    @Override
    protected SyntaxHighlighter createHighlighter() {
        return new ScalaDocSyntaxHighlighter();
    }

    @Override
    public Language getLanguage() {
        return ScalaDocLanguage.INSTANCE;
    }
}
