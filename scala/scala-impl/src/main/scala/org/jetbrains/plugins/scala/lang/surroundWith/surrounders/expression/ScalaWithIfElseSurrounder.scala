package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import consulo.localize.LocalizeValue

import consulo.language.psi.PsiElement

/*
 * Surrounds expression with { } and if: if { <Cursor> } { Expression }
 */
class ScalaWithIfElseSurrounder extends ScalaWithIfSurrounderBase {
  override def getTemplateAsString(elements: Array[PsiElement]): String =
    "if (a) { " + super.getTemplateAsString(elements) + "} else {  }"

  //noinspection ScalaExtractStringToBundle,DialogTitleCapitalization
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of("if / else")

  override protected val isApplicableToMultipleElements: Boolean = true
}
