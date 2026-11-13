package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.scaladoc

import consulo.localize.LocalizeValue

import org.jetbrains.plugins.scala.ScalaBundle

class ScalaDocWithBoldSurrounder extends ScalaDocWithSyntaxSurrounder {
  override def getTemplateDescription: LocalizeValue = LocalizeValue.of(ScalaBundle.message("bold.surrounder.template.description"))

  override def getSyntaxTag = "'''"
}
