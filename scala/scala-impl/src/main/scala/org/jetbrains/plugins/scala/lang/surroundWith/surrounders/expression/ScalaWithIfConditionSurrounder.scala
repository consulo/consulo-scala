package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.localize.LocalizeValue

import consulo.language.psi.PsiElement

object ScalaWithIfConditionSurrounder extends ScalaWithIfConditionSurrounderBase {
  override def getTemplateAsString(elements: Array[PsiElement]): String =
    "if (" + super.getTemplateAsString(elements) + ") {}"

  //noinspection ScalaExtractStringToBundle,DialogTitleCapitalization
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of("if (expr) {...}")
}
