package org.jetbrains.plugins.scala;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaBundle";
    private static final AbstractBundle INSTANCE = new AbstractBundle(ScalaBundle.class, BUNDLE);

    private ScalaBundle() {
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
