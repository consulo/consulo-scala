package org.jetbrains.plugins.scala.lang.scaladoc.psi.api

import com.intellij.java.language.psi.javadoc.PsiDocTagValue
import consulo.language.psi.{PsiElement, PsiNamedElement}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPolyResolvable
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createScalaDocTagValue

trait ScDocTagValue extends PsiDocTagValue with ScPolyResolvable with PsiNamedElement {

  override def setName(name: String): PsiElement = {
    replace(createScalaDocTagValue(name)(using getManager))
    this
  }
}