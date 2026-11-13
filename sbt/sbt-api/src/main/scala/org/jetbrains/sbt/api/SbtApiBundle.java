package org.jetbrains.sbt.api;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

// upstream: DynamicBundle instance field, Consulo: AbstractBundle subclass (same as consulo-groovy bundles)
public final class SbtApiBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "messages.SbtApiBundle";
    private static final SbtApiBundle INSTANCE = new SbtApiBundle();

    private SbtApiBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
