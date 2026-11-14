// This is a generated file. Not intended for manual editing.
package org.jetbrains.sbt.shell.grammar.impl;

import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import org.jetbrains.sbt.shell.grammar.SbtShellUri;
import org.jetbrains.sbt.shell.grammar.SbtShellVisitor;

import static org.jetbrains.sbt.shell.grammar.SbtShellTypes.URISTRING;

public class SbtShellUriImpl extends ASTWrapperPsiElement implements SbtShellUri {

  public SbtShellUriImpl(ASTNode node) {
    super(node);
  }

  public void accept(SbtShellVisitor visitor) {
    visitor.visitUri(this);
  }

  @Override
  public void accept(PsiElementVisitor visitor) {
    if (visitor instanceof SbtShellVisitor) accept((SbtShellVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  public PsiElement getUristring() {
    return findNotNullChildByType(URISTRING);
  }

}
