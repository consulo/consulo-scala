package org.jetbrains.plugins.scala.munit.testingSupport.test.munit;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class MUnitTestingSupportBundle extends AbstractBundle {
    @NonNls
    public static final String BUNDLE = "org.jetbrains.plugins.scala.munit.testingSupport.test.munit.MUnitTestingSupportBundle";
    private static final MUnitTestingSupportBundle INSTANCE = new MUnitTestingSupportBundle();

    private MUnitTestingSupportBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
