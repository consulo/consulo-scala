package org.jetbrains.plugins.scala.highlighter;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaOptionsBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaOptionsBundle";
    private static final DynamicBundle INSTANCE = new DynamicBundle(ScalaOptionsBundle.class, BUNDLE);

    private ScalaOptionsBundle() {
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
