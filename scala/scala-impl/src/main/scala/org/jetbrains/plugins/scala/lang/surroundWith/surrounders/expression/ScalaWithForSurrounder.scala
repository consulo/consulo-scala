package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.localize.LocalizeValue

import consulo.language.psi.PsiElement

class ScalaWithForSurrounder extends ScalaWithForSurrounderBase {
  //noinspection ScalaExtractStringToBundle,DialogTitleCapitalization
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of("for")

  override def getTemplateAsString(elements: Array[PsiElement]): String = {
    "for (a <- as) {" + super.getTemplateAsString(elements) + "}"
  }
}
