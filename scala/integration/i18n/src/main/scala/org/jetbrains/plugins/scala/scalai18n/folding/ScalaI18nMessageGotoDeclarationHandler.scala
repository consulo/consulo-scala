package org.jetbrains.plugins.scala.scalai18n.folding

import consulo.annotation.component.ExtensionImpl
import consulo.codeEditor.Editor
import consulo.java.properties.impl.i18n.folding.EditPropertyValueAction
import consulo.language.editor.navigation.GotoDeclarationHandlerBase
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.literals.ScStringLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall

/**
 * Navigates from a folded `"key"` / `Bundle.message("key", ...)` to the property.
 */
@ExtensionImpl
final class ScalaI18nMessageGotoDeclarationHandler extends GotoDeclarationHandlerBase {

  override def getGotoDeclarationTarget(element: PsiElement, editor: Editor): PsiElement = {
    if (element == null || !element.getContainingFile.isInstanceOf[ScalaFile]) return null
    val region = editor.getFoldingModel.getCollapsedRegionAtOffset(element.getTextRange.getStartOffset)
    if (region == null) return null

    EditPropertyValueAction.getEditableElement(region) match {
      case literal: ScStringLiteral => resolve(literal)
      case call: ScMethodCall => call.argumentExpressions.headOption.map(resolve).orNull
      case _ => null
    }
  }

  private def resolve(element: PsiElement): PsiElement =
    element.getReferences.headOption.map(_.resolve()).orNull
}
