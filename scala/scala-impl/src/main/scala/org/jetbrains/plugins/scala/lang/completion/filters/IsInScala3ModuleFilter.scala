package org.jetbrains.plugins.scala.lang.completion.filters

import consulo.language.psi.PsiElement
import consulo.language.psi.filter.ElementFilter
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt

object IsInScala3ModuleFilter extends ElementFilter {
  override def toString: String = "IsInScala3ModuleFilter"

  override def isClassAcceptable(hintClass: Class[?]): Boolean = true

  override def isAcceptable(element: Object, @Nullable context: PsiElement): Boolean =
    context != null && context.isInScala3Module
}
