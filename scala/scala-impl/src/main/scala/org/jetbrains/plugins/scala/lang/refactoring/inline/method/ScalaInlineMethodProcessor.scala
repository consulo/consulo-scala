package org.jetbrains.plugins.scala.lang.refactoring.inline.method

import consulo.localize.LocalizeValue
import consulo.project.Project
import consulo.language.psi.PsiReference
import consulo.language.impl.psi.CodeEditUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.refactoring.inline.ScalaInlineProcessor

final class ScalaInlineMethodProcessor(
  method: ScFunctionDefinition,
  reference: Option[PsiReference],
  inlineThisOnly: Boolean,
  removeDefinitionAfterInlining: Boolean
)(
  implicit project: Project
) extends ScalaInlineProcessor(method, reference, inlineThisOnly = inlineThisOnly, shouldRemoveDefinition = removeDefinitionAfterInlining) {
  override def getCommandName: LocalizeValue = LocalizeValue.of(ScalaInlineMethodHandler.RefactoringName)

  override protected def removeDefinition(): Unit =
    CodeEditUtil.removeChild(method.getParent.getNode, method.getNode)

  override protected val processedElementsUsageViewHeader: String =
    if (method.isLocal) ScalaBundle.message("inline.local.method.elements.header")
    else ScalaBundle.message("inline.method.elements.header")
}
