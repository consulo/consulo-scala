package org.jetbrains.plugins.scala.codeInspection;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaInspectionBundle extends AbstractBundle {
  @NonNls
  private static final String BUNDLE = "messages.ScalaInspectionBundle";
  private static final ScalaInspectionBundle INSTANCE = new ScalaInspectionBundle();

  private ScalaInspectionBundle() {
        super(BUNDLE);
    }

  @Nls
  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    String message = INSTANCE.getMessage(key, params);
        return message != null ? message : key;
  }
}
