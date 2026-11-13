package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.scaladoc

import consulo.localize.LocalizeValue

import org.jetbrains.plugins.scala.ScalaBundle

class ScalaDocWithUnderlinedSurrounder extends ScalaDocWithSyntaxSurrounder {
  override def getSyntaxTag = "__"

  override def getTemplateDescription: LocalizeValue = LocalizeValue.of(ScalaBundle.message("underline.surrounder.template.description"))
}
