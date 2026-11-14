// This is a generated file. Not intended for manual editing.
package org.jetbrains.sbt.shell.grammar.impl;

import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElementVisitor;
import org.jspecify.annotations.Nullable;
import org.jetbrains.sbt.shell.grammar.*;

public class SbtShellScopedKeyImpl extends ASTWrapperPsiElement implements SbtShellScopedKey {

  public SbtShellScopedKeyImpl(ASTNode node) {
    super(node);
  }

  public void accept(SbtShellVisitor visitor) {
    visitor.visitScopedKey(this);
  }

  @Override
  public void accept(PsiElementVisitor visitor) {
    if (visitor instanceof SbtShellVisitor) accept((SbtShellVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public SbtShellConfig getConfig() {
    return findChildByClass(SbtShellConfig.class);
  }

  @Override
  @Nullable
  public SbtShellIntask getIntask() {
    return findChildByClass(SbtShellIntask.class);
  }

  @Override
  public SbtShellKey getKey() {
    return findNotNullChildByClass(SbtShellKey.class);
  }

  @Override
  @Nullable
  public SbtShellProjectId getProjectId() {
    return findChildByClass(SbtShellProjectId.class);
  }

  @Override
  @Nullable
  public SbtShellUri getUri() {
    return findChildByClass(SbtShellUri.class);
  }

}
