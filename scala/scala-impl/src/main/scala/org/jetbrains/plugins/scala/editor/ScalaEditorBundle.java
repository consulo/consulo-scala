package org.jetbrains.plugins.scala.editor;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaEditorBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaEditorBundle";
    private static final AbstractBundle INSTANCE = new AbstractBundle(ScalaEditorBundle.class, BUNDLE);

    private ScalaEditorBundle() {
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
