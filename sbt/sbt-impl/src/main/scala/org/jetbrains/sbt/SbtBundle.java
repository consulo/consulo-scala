package org.jetbrains.sbt;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class SbtBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "org.jetbrains.sbt.SbtBundle";
    private static final SbtBundle INSTANCE = new SbtBundle();

    private SbtBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
