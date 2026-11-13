package org.jetbrains.plugins.scala.extensions

import consulo.language.psi.PsiFile
import com.intellij.java.language.psi.PsiMember

object ContainingFile {
  def unapply(e: PsiMember): Option[PsiFile] =
    Option(e.getContainingFile)
}