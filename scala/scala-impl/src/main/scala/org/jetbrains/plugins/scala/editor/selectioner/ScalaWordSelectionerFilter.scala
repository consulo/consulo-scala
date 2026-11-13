package org.jetbrains.plugins.scala.editor.selectioner

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.action.WordSelectionerFilter
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.ScInterpolatedStringLiteral

@ExtensionImpl
class ScalaWordSelectionerFilter extends WordSelectionerFilter {

  override def canSelect(e: PsiElement): Boolean = e.getParent match {
    case _: ScInterpolatedStringLiteral => false //handled by ScalaStringLiteralSelectioner
    case _                              => true
  }
}
