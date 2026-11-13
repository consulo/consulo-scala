package org.jetbrains.plugins.scala.lang.refactoring.util;

import org.jspecify.annotations.Nullable;

public interface NamedDialog {
  public boolean isReplaceAllOccurrences();

  @Nullable
  public String getEnteredName();
}
