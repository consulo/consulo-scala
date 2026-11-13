package org.jetbrains.plugins.scala.highlighter;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaOptionsBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaOptionsBundle";
    private static final AbstractBundle INSTANCE = new AbstractBundle(ScalaOptionsBundle.class, BUNDLE);

    private ScalaOptionsBundle() {
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
