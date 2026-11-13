package org.jetbrains.plugins.scala.lang.psi.impl.toplevel
package synthetic

import consulo.document.util.TextRange
import com.intellij.java.language.impl.psi.impl.light.LightIdentifier
import consulo.language.psi.{PsiElement, PsiFile}

class JavaIdentifier(scalaId : PsiElement) extends LightIdentifier(scalaId.getManager, scalaId.getText) {
  override def getTextRange: TextRange = scalaId.getTextRange

  override def getStartOffsetInParent: Int = scalaId.getStartOffsetInParent

  override def getTextOffset: Int = scalaId.getTextOffset

  override def getContainingFile: PsiFile = scalaId.getContainingFile
}