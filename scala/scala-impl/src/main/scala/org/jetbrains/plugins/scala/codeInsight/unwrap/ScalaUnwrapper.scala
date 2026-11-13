package org.jetbrains.plugins.scala.codeInsight.unwrap

import consulo.language.editor.CodeInsightBundle
import consulo.language.editor.refactoring.unwrap.AbstractUnwrapper
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.StringExt

abstract class ScalaUnwrapper extends AbstractUnwrapper[ScalaUnwrapContext]("") {
  override def createContext(): ScalaUnwrapContext = new ScalaUnwrapContext
}

trait ShortTextDescription {
  this: ScalaUnwrapper =>

  protected def shortText(element: PsiElement): String =
    element.getText.shorten(20)

  override def getDescription(e: PsiElement): String =
    CodeInsightBundle.message("unwrap.with.placeholder", shortText(e))
}
