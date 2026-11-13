package org.jetbrains.plugins.scala.editor;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaEditorBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaEditorBundle";
    private static final ScalaEditorBundle INSTANCE = new ScalaEditorBundle();

    private ScalaEditorBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
