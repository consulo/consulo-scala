package org.jetbrains.plugins.scala;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaBundle";
    private static final ScalaBundle INSTANCE = new ScalaBundle();

    private ScalaBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
