package org.jetbrains.plugins.scala.codeInsight.implicits;

import com.intellij.codeInsight.daemon.impl.HintRenderer;
import consulo.ui.ex.action.ActionGroup;
import consulo.codeEditor.Editor;
import com.intellij.openapi.editor.Inlay;
import consulo.colorScheme.TextAttributes;
import org.jspecify.annotations.Nullable;

import java.awt.*;

// A workaround for the raw Inlay Java types.
// Given that a renderer is created in the context of Editor & Inlay,
// both the Editor and the Inlay can be accepted as constructor arguments.
// There's no real need to supply Editor / Inlay as method parameters.
@SuppressWarnings("ALL") // The Inlay class is now considered "an experimental API" anyway
abstract class HintRendererProxy extends HintRenderer {
    public HintRendererProxy(String text) {
        super(text);
    }

    protected abstract int calcWidthInPixels0(Editor editor);

    @Override
    final public int calcWidthInPixels(Inlay inlay) {
        return calcWidthInPixels0(inlay.getEditor());
    }

    protected abstract void paint0(Editor editor, Graphics g, Rectangle r, TextAttributes textAttributes);

    @Override
    final public void paint(Inlay inlay, Graphics g, Rectangle r, TextAttributes textAttributes) {
        paint0(inlay.getEditor(), g, r, textAttributes);
    }

    @Nullable
    protected abstract String getContextMenuGroupId0(Editor editor);

    @Nullable
    @Override
    final public String getContextMenuGroupId(Inlay inlay) {
        return getContextMenuGroupId0(inlay.getEditor());
    }

    @Nullable
    protected abstract ActionGroup getContextMenuGroup0(Editor editor);

    @Nullable
    @Override
    final public ActionGroup getContextMenuGroup(Inlay inlay) {
        return getContextMenuGroup0(inlay.getEditor());
    }
}
