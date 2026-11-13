package org.jetbrains.plugins.scala.lang.completion.filters.expression

import consulo.language.psi.filter.ElementFilter
import consulo.language.psi.{PsiComment, PsiElement}
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.annotations.{NonNls, Nullable}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr._

class MatchFilter extends ElementFilter {
  override def isAcceptable(element: Object, @Nullable context: PsiElement): Boolean = {
    if (context == null || context.is[PsiComment]) return false
    val leaf = PsiTreeUtil.getDeepestFirst(context)
    if (leaf != null) {
      val parent = leaf.getParent
      if (parent.is[ScExpression] &&
        !parent.is[ScStringLiteral] &&
        (parent.getParent.is[ScInfixExpr] ||
          (parent.getParent.is[ScPostfixExpr] &&
            !parent.getParent.getParent.is[ScTry]))) {
        return true
      }
    }
    false
  }

  override def isClassAcceptable(hintClass: java.lang.Class[?]): Boolean = {
    true
  }

  @NonNls
  override def toString: String = {
    "'match' keyword filter"
  }
}