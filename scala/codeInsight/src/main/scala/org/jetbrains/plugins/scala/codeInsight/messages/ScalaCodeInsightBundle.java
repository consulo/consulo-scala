package org.jetbrains.plugins.scala.codeInsight.messages;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaCodeInsightBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle";
    private static final ScalaCodeInsightBundle INSTANCE = new ScalaCodeInsightBundle();

    private ScalaCodeInsightBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
