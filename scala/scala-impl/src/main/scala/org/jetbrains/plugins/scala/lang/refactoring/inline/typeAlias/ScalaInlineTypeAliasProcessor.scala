package org.jetbrains.plugins.scala.lang.refactoring.inline.typeAlias

import consulo.project.Project
import consulo.language.psi.PsiReference
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAliasDefinition
import org.jetbrains.plugins.scala.lang.refactoring.inline.ScalaInlineProcessor

final class ScalaInlineTypeAliasProcessor(
  typeAlias: ScTypeAliasDefinition,
  reference: Option[PsiReference],
  inlineThisOnly: Boolean,
  removeDefinitionAfterInlining: Boolean
)(
  implicit project: Project
) extends ScalaInlineProcessor(typeAlias, reference, inlineThisOnly = inlineThisOnly, shouldRemoveDefinition = removeDefinitionAfterInlining) {
  override def getCommandName: String = ScalaInlineTypeAliasHandler.RefactoringName

  override protected def removeDefinition(): Unit =
    removeElementWithNonSignificantSiblings(typeAlias)

  override protected val processedElementsUsageViewHeader: String =
    if (typeAlias.isLocal) ScalaBundle.message("inline.local.type.alias.elements.header")
    else ScalaBundle.message("inline.type.alias.elements.header")
}
