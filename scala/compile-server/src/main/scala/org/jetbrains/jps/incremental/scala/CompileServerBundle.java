package org.jetbrains.jps.incremental.scala;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class CompileServerBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "org.jetbrains.jps.incremental.scala.ScalaCompileServerBundle";
    private static final CompileServerBundle INSTANCE = new CompileServerBundle();

    private CompileServerBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
