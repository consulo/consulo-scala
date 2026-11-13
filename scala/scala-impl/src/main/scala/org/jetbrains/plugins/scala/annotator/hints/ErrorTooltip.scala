package org.jetbrains.plugins.scala.annotator.hints

import com.intellij.codeInsight.hint.TooltipGroup
import consulo.language.editor.intention.IntentionAction
import consulo.language.psi.PsiElement
import org.jetbrains.annotations.Nls

import scala.language.implicitConversions

sealed trait ErrorTooltip {
  @Nls
  def message: String
}

object ErrorTooltip {
  val tooltipGroup = new TooltipGroup("Scala inlay error tooltip", 0)

  implicit def fromString(@Nls message: String): ErrorTooltip = ErrorTooltip(message)

  def apply(@Nls message: String): ErrorTooltip = JustText(message)

  def apply(@Nls message: String, action: IntentionAction, element: PsiElement): ErrorTooltip = {
    WithAction(message, action, element)
  }

  case class JustText(override val message: String) extends ErrorTooltip

  case class WithAction(override val message: String, action: IntentionAction, element: PsiElement) extends ErrorTooltip
}
