package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.language.psi.PsiElement

class ScalaWithIfElseConditionSurrounder extends ScalaWithIfConditionSurrounderBase {
  override def getTemplateAsString(elements: Array[PsiElement]): String =
    "if (" + super.getTemplateAsString(elements) + ") {} else {}"

  //noinspection ScalaExtractStringToBundle,DialogTitleCapitalization
  override def getTemplateDescription: String = "if (expr) {...} else {...}"
}
