package org.jetbrains.plugins.scala.lang.completion.filters.toplevel

import consulo.language.psi.filter.ElementFilter
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.{PsiComment, PsiElement}
import com.intellij.java.language.psi.PsiIdentifier
import org.jetbrains.annotations.{NonNls, Nullable}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil._

class ExtensionFilter extends ElementFilter {
  override def isAcceptable(element: Object, @Nullable context: PsiElement): Boolean = {
    if (context == null || !context.isInScala3File || context.is[PsiComment, PsiIdentifier]) return false
    val leaf = PsiTreeUtil.getDeepestFirst(context)

    val parent = leaf.getParent
    getForAll(parent, leaf) match {
      case (true, result) => result
      case (false, _) => false
    }
  }

  override def isClassAcceptable(hintClass: Class[?]): Boolean = true

  @NonNls
  override def toString: String = "extension keyword filter"
}
