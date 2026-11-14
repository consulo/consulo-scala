package org.jetbrains.bsp;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class BspBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "org.jetbrains.bsp.ScalaBspBundle";
    private static final BspBundle INSTANCE = new BspBundle();

    private BspBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
