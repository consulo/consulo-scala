package org.jetbrains.scalaCli;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaCliBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "org.jetbrains.scalaCli.ScalaCliBundle";
    private static final ScalaCliBundle INSTANCE = new ScalaCliBundle();

    private ScalaCliBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
