package org.jetbrains.plugins.scala.lang.completion.filters.definitions

import consulo.language.psi.filter.ElementFilter
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.{PsiComment, PsiElement, PsiErrorElement, PsiWhiteSpace}
import org.jetbrains.annotations.{NonNls, Nullable}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.parser.ErrMsg
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScExtensionBody

class ExtensionDefFilter extends ElementFilter {
  override def isAcceptable(element: Object, @Nullable context: PsiElement): Boolean = {
    if (context == null || context.is[PsiComment]) return false
    val leaf = PsiTreeUtil.getDeepestFirst(context)

    if (leaf.getParent.is[ScExtensionBody]) {
      val errorBeforeDefStart = leaf.prevLeafs.filterNot(_.is[PsiComment, PsiWhiteSpace]).nextOption()

      errorBeforeDefStart match {
        case Some(err: PsiErrorElement) if err.getErrorDescription == ErrMsg("extension.method.expected") =>
          return true
        case _ =>
      }
    }

    false
  }

  override def isClassAcceptable(hintClass: Class[?]): Boolean = true

  @NonNls
  override def toString: String = "'def' keyword in extensions filter"
}
