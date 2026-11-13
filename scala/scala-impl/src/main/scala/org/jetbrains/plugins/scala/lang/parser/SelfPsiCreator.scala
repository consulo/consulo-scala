package org.jetbrains.plugins.scala.lang.parser

import consulo.language.ast.ASTNode
import consulo.language.psi.PsiElement

trait SelfPsiCreator {
  def createElement(node: ASTNode): PsiElement
}