package org.jetbrains.plugins.scala.lang.macros.expansion

import consulo.language.editor.intention.IntentionAction
import org.jetbrains.annotations.NonNls

abstract class ScalaMetaIntentionAction extends IntentionAction {
  @NonNls
  def getFamilyName: String = "Scala.meta"

  override def startInWriteAction(): Boolean = true
}
