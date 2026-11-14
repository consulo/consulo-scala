package org.jetbrains.plugins.scala.codeInsight.template.macros

import consulo.language.editor.template.Result
import consulo.document.Document
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.lang.psi.types.ScType

case class ScalaTypeResult(`type`: ScType) extends Result {

  //TODO maybe add a more meaningful implementation
  override def equalsToText(text: String, context: PsiElement): Boolean =
    text == toString

  override def toString: String = `type`.canonicalText

  override def handleFocused(psiFile: PsiFile,
                             document: Document,
                             segmentStart: Int,
                             segmentEnd: Int): Unit = {}
}
