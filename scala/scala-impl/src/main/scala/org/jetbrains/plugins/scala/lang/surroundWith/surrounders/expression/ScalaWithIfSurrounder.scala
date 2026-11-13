package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.localize.LocalizeValue

import consulo.language.psi.PsiElement

/*
 * Surrounds expression with if: if { <Cursor> } Expression
 */
class ScalaWithIfSurrounder extends ScalaWithIfSurrounderBase {

  override def getTemplateAsString(elements: Array[PsiElement]): String =
    "if (true) {\n" + super.getTemplateAsString(elements) + "\n}"

  //noinspection ScalaExtractStringToBundle,DialogTitleCapitalization
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of("if")

  override protected val isApplicableToMultipleElements: Boolean = true
}
