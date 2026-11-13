package org.jetbrains.plugins.scala.lang.scaladoc.psi.impl

import consulo.language.psi.PsiReference
import consulo.language.psi.ReferenceProvidersRegistry
import consulo.language.impl.psi.LeafPsiElement
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocElementType
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScPsiDocToken

class ScPsiDocTokenImpl(
  override val tokenType: ScalaDocElementType,
  text: CharSequence
) extends LeafPsiElement(tokenType, text) with ScPsiDocToken {

  override def toString: String = s"ScPsiDocToken(${tokenType.toString})"

  override def getReferences: Array[PsiReference] =
    ReferenceProvidersRegistry.getReferencesFromProviders(this)
}
