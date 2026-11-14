// This is a generated file. Not intended for manual editing.
package org.jetbrains.sbt.shell.grammar.impl;

import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import org.jetbrains.sbt.shell.grammar.SbtShellConfig;
import org.jetbrains.sbt.shell.grammar.SbtShellVisitor;

import static org.jetbrains.sbt.shell.grammar.SbtShellTypes.ID;

public class SbtShellConfigImpl extends ASTWrapperPsiElement implements SbtShellConfig {

  public SbtShellConfigImpl(ASTNode node) {
    super(node);
  }

  public void accept(SbtShellVisitor visitor) {
    visitor.visitConfig(this);
  }

  @Override
  public void accept(PsiElementVisitor visitor) {
    if (visitor instanceof SbtShellVisitor) accept((SbtShellVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  public PsiElement getId() {
    return findNotNullChildByType(ID);
  }

}
