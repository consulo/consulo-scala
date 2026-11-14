// This is a generated file. Not intended for manual editing.
package org.jetbrains.sbt.shell.grammar;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public interface SbtShellScopedKey extends PsiElement {

  @Nullable
  SbtShellConfig getConfig();

  @Nullable
  SbtShellIntask getIntask();

  SbtShellKey getKey();

  @Nullable
  SbtShellProjectId getProjectId();

  @Nullable
  SbtShellUri getUri();

}
