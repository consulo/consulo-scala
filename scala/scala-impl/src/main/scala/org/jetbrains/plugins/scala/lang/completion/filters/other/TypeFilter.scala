package org.jetbrains.plugins.scala.lang.completion.filters.other

import consulo.language.psi.filter.ElementFilter
import consulo.language.psi.{PsiComment, PsiElement}
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportStmt

class TypeFilter extends ElementFilter {
  override def isAcceptable(element: Object, @Nullable context: PsiElement): Boolean = {
    if (context == null || context.isInstanceOf[PsiComment]) return false
    val leaf = PsiTreeUtil.getDeepestFirst(context)

    val imp = leaf.parentOfType(classOf[ScImportStmt], strict = false)
    if (imp.isDefined) return false

    if (leaf != null) {
      val parent = leaf.getParent
      parent match {
        case _: ScStableCodeReference => return true
        case _ => return false
      }
    }
    false
  }

  override def toString: String = "'type' keyword filter"

  override def isClassAcceptable(hintClass: java.lang.Class[?]): Boolean = true
}