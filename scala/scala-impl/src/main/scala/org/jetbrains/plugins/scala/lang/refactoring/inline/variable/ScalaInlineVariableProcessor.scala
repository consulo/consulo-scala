package org.jetbrains.plugins.scala.lang.refactoring.inline.variable

import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.language.psi.PsiReference
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariableDefinition
import org.jetbrains.plugins.scala.lang.refactoring.inline.ScalaInlineProcessor

final class ScalaInlineVariableProcessor(
  pattern: ScBindingPattern,
  variable: ScValueOrVariableDefinition,
  reference: Option[PsiReference],
  inlineThisOnly: Boolean,
  removeDefinitionAfterInlining: Boolean
)(
  implicit project: Project
) extends ScalaInlineProcessor(pattern, reference, inlineThisOnly = inlineThisOnly, shouldRemoveDefinition = removeDefinitionAfterInlining) {
  override def getCommandName: LocalizeValue = LocalizeValue.of(ScalaInlineVariableHandler.RefactoringName)

  override protected def removeDefinition(): Unit =
    removeElementWithNonSignificantSiblings(variable)

  override protected val processedElementsUsageViewHeader: String =
    if (variable.isLocal) ScalaBundle.message("inline.local.variable.elements.header")
    else ScalaBundle.message("inline.variable.elements.header")
}
