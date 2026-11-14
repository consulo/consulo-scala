package org.jetbrains.plugins.scala.annotator.quickfix

import consulo.language.editor.intention.SyntheticIntentionAction
import consulo.localize.LocalizeValue
import consulo.language.editor.inspection.FileModifier
import consulo.language.editor.intention.IntentionAction
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiFile
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScSymbolLiteral
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

final class ConvertToExplicitSymbolQuickFix(symbolLiteral: ScSymbolLiteral)
  extends SyntheticIntentionAction
    with DumbAware {
  private val symbolText = symbolLiteral.contentText

  override def getText: LocalizeValue = LocalizeValue.of(ScalaBundle.message("convert.to.explicit.symbol", symbolText))

  def getFamilyName: String = ScalaBundle.message("convert.to.explicit.symbol.family")

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean =
    symbolLiteral.isValid

  override def invoke(project: Project,
                      editor: Editor,
                      file: PsiFile): Unit =
    if (symbolLiteral.isValid) {
      val newText = s"""Symbol("$symbolText")"""
      symbolLiteral.replace {
        ScalaPsiElementFactory.createExpressionFromText(newText, symbolLiteral)(using project)
      }
    }

  override def startInWriteAction: Boolean = true

  override def getFileModifierForPreview(target: PsiFile): FileModifier =
    new ConvertToExplicitSymbolQuickFix(PsiTreeUtil.findSameElementInCopy(symbolLiteral, target))
}
