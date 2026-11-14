package org.jetbrains.plugins.scala.codeInsight.implicits;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.Inlay;
import consulo.colorScheme.TextAttributes;
import org.jspecify.annotations.Nullable;

import java.awt.*;

// A workaround for the raw Inlay Java types.
// Given that a renderer is created in the context of Editor & Inlay,
// both the Editor and the Inlay can be accepted as constructor arguments.
// There's no real need to supply Editor / Inlay as method parameters.
// upstream: extends com.intellij.codeInsight.daemon.impl.HintRenderer (only the text and the text attributes are used)
abstract class HintRendererProxy implements EditorCustomElementRenderer {
    private String text;

    public HintRendererProxy(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    protected TextAttributes getTextAttributes(Editor editor) {
        return editor.getColorsScheme().getAttributes(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT);
    }

    protected abstract int calcWidthInPixels0(Editor editor);

    @Override
    final public int calcWidthInPixels(Inlay<?> inlay) {
        return calcWidthInPixels0(inlay.getEditor());
    }

    protected abstract void paint0(Editor editor, Graphics g, Rectangle r, TextAttributes textAttributes);

    @Override
    final public void paint(Inlay<?> inlay, Graphics g, Rectangle r, TextAttributes textAttributes) {
        paint0(inlay.getEditor(), g, r, textAttributes);
    }

    @Nullable
    protected abstract String getContextMenuGroupId0(Editor editor);

    @Nullable
    @Override
    final public String getContextMenuGroupId(Inlay<?> inlay) {
        return getContextMenuGroupId0(inlay.getEditor());
    }
}
