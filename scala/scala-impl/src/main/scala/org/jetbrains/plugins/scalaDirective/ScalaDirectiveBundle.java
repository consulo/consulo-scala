package org.jetbrains.plugins.scalaDirective;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaDirectiveBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaDirectiveBundle";
    private static final AbstractBundle INSTANCE = new AbstractBundle(ScalaDirectiveBundle.class, BUNDLE);

    private ScalaDirectiveBundle() {
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
