package org.jetbrains.plugins.scalaDirective.highlighter;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;
import org.jetbrains.plugins.scalaDirective.ScalaDirectiveLanguage;

@ExtensionImpl
public final class ScalaDirectiveSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {
    @Override
    protected SyntaxHighlighter createHighlighter() {
        return new ScalaDirectiveSyntaxHighlighter();
    }

    @Override
    public Language getLanguage() {
        return ScalaDirectiveLanguage.INSTANCE;
    }
}
