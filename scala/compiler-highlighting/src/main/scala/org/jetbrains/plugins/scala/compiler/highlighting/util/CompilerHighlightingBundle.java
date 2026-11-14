package org.jetbrains.plugins.scala.compiler.highlighting.util;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class CompilerHighlightingBundle extends AbstractBundle {
    // upstream: messages.CompilerHighlightingBundle, the messages package would be split between the plugin modules
    @NonNls
    private static final String BUNDLE = "org.jetbrains.plugins.scala.compiler.highlighting.util.CompilerHighlightingBundle";
    private static final CompilerHighlightingBundle INSTANCE = new CompilerHighlightingBundle();

    private CompilerHighlightingBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
