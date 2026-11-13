package org.jetbrains.plugins.scala.lang.psi.impl.base.types

import consulo.language.ast.ASTNode
import consulo.language.psi.resolve.PsiScopeProcessor
import consulo.language.psi.PsiElement
import consulo.language.psi.resolve.ResolveState
import org.jetbrains.plugins.scala.extensions.{IterableOnceExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.base.types.{ScMatchTypeCase, ScTypeElement, ScTypeVariableTypeElement}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl

class ScMatchTypeCaseImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScMatchTypeCase {

  override def pattern: Option[ScTypeElement] = findChild[ScTypeElement]

  override def result: Option[ScTypeElement] = findLastChild[ScTypeElement]

  override def processDeclarations(processor: PsiScopeProcessor, state : ResolveState, lastParent: PsiElement, place: PsiElement): Boolean = {
    (pattern, result) match {
      case (Some(p), Some(r)) if lastParent != null && r.startOffsetInParent == lastParent.startOffsetInParent =>
        val typeVariables = p.elements.filterByType[ScTypeVariableTypeElement]
        typeVariables.forall(processor.execute(_, state))
      case _ => true
    }
  }
}
