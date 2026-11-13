package org.jetbrains.plugins.scala.lang.completion.filters.toplevel

import consulo.language.psi.{PsiComment, PsiElement}
import consulo.language.psi.filter.ElementFilter
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.annotations.{NonNls, Nullable}
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil._

class TraitFilter extends ElementFilter {
  override def isAcceptable(element: Object, @Nullable context: PsiElement): Boolean = {
    if (context == null || context.is[PsiComment]) return false
    val leaf = PsiTreeUtil.getDeepestFirst(context)

    val parent = leaf.getParent
    ScalaCompletionUtil.getForAll(parent, leaf) match {
      case (true, result) => result
      case (false, _) => checkAfterSoftModifier(parent, leaf)
    }
  }

  override def isClassAcceptable(hintClass: java.lang.Class[?]): Boolean = true

  @NonNls
  override def toString = "template definitions keyword filter"
}