package org.jetbrains.plugins.scala.integration.compiler;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class CompilerIntegrationBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "org.jetbrains.plugins.scala.integration.compiler.CompilerIntegrationBundle";
    private static final CompilerIntegrationBundle INSTANCE = new CompilerIntegrationBundle();

    private CompilerIntegrationBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
