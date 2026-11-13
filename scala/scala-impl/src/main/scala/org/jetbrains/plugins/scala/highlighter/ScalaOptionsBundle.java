package org.jetbrains.plugins.scala.highlighter;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaOptionsBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaOptionsBundle";
    private static final ScalaOptionsBundle INSTANCE = new ScalaOptionsBundle();

    private ScalaOptionsBundle() {
        super(BUNDLE);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
