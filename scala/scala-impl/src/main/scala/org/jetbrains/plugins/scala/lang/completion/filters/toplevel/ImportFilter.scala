package org.jetbrains.plugins.scala.lang.completion.filters.toplevel

import consulo.language.psi.filter.ElementFilter
import consulo.language.psi.{PsiComment, PsiElement}
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.annotations.{NonNls, Nullable}
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil

class ImportFilter extends ElementFilter {
  override def isAcceptable(element: Object, @Nullable context: PsiElement): Boolean = {
    if (context == null || context.isInstanceOf[PsiComment]) return false
    val leaf = PsiTreeUtil.getDeepestFirst(context)
    
    if (leaf != null) {
      val parent = leaf.getParent
      val tuple = ScalaCompletionUtil.getForAll(parent,leaf)
      if (tuple._1) return tuple._2
    }
    false
  }

  override def isClassAcceptable(hintClass: java.lang.Class[?]): Boolean = true

  @NonNls
  override def toString = "'import' keyword filter"
}