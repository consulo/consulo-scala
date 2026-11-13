package org.jetbrains.plugins.scalaDirective;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaDirectiveBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "messages.ScalaDirectiveBundle";
    private static final ScalaDirectiveBundle INSTANCE = new ScalaDirectiveBundle();

    private ScalaDirectiveBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
