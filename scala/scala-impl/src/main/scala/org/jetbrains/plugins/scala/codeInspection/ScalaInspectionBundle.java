package org.jetbrains.plugins.scala.codeInspection;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class ScalaInspectionBundle {
  @NonNls
  private static final String BUNDLE = "messages.ScalaInspectionBundle";
  private static final AbstractBundle INSTANCE = new AbstractBundle(ScalaInspectionBundle.class, BUNDLE);

  private ScalaInspectionBundle() {
  }

  @Nls
  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getMessage(key, params);
  }
}
