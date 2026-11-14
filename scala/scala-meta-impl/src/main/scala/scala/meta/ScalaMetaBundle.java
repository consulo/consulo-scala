package scala.meta;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaMetaBundle extends AbstractBundle {
    @NonNls
    private static final String BUNDLE = "scala.meta.ScalaMetaBundle";
    private static final ScalaMetaBundle INSTANCE = new ScalaMetaBundle();

    private ScalaMetaBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
    }
}
