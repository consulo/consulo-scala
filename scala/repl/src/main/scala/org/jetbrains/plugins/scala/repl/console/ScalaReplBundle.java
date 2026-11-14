package org.jetbrains.plugins.scala.repl.console;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaReplBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "org.jetbrains.plugins.scala.repl.console.ScalaReplBundle";
    private static final ScalaReplBundle INSTANCE = new ScalaReplBundle();

    private ScalaReplBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
