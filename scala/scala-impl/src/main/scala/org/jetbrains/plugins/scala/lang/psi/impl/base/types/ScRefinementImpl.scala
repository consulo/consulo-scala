package org.jetbrains.plugins.scala.lang.psi.impl.base
package types

import consulo.language.ast.ASTNode
import consulo.language.psi.resolve.PsiScopeProcessor
import consulo.language.psi.PsiElement
import consulo.language.psi.resolve.ResolveState
import org.jetbrains.plugins.scala.lang.psi.api.base.types._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl

class ScRefinementImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScRefinement{
  override def toString: String = "Refinement"

  override def processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement, place: PsiElement): Boolean = {
    val iterator = types.iterator
    while (iterator.hasNext) {
      val elem = iterator.next()
      if (!processor.execute(elem, state)) return false
    }

    val iterator1 = holders.iterator.flatMap(_.declaredElements.iterator)
    while (iterator1.hasNext) {
      val elem = iterator1.next()
      if (!processor.execute(elem, state)) return false
    }
    true
  }
}