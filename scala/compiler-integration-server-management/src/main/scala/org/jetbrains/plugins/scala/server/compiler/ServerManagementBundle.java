package org.jetbrains.plugins.scala.server.compiler;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ServerManagementBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "org.jetbrains.plugins.scala.server.compiler.ServerManagementBundle";
    private static final ServerManagementBundle INSTANCE = new ServerManagementBundle();

    private ServerManagementBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
