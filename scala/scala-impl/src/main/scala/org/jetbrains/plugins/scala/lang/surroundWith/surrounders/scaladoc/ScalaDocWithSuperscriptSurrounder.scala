package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.scaladoc

import consulo.localize.LocalizeValue

import org.jetbrains.plugins.scala.ScalaBundle

class ScalaDocWithSuperscriptSurrounder extends ScalaDocWithSyntaxSurrounder {
  override def getSyntaxTag: String = "^"

  override def getTemplateDescription: LocalizeValue = LocalizeValue.of(ScalaBundle.message("superscript.surrounder.template.description"))
}
