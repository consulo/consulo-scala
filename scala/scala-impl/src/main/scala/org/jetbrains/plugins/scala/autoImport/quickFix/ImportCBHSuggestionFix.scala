package org.jetbrains.plugins.scala.autoImport.quickFix

import consulo.localize.LocalizeValue
import consulo.codeEditor.Editor
import consulo.language.psi.PsiElement
import org.jetbrains.plugins.scala.ScalaBundle

final class ImportCBHSuggestionFix private(variants: Seq[CBHSuggestionToImport], place: PsiElement)
  extends ScalaImportElementFix[CBHSuggestionToImport](place)
{
  override protected def findElementsToImport(): Seq[CBHSuggestionToImport] = variants

  override def createAddImportAction(editor: Editor): ScalaAddImportAction[?, ?] = ScalaAddImportAction.cbhSuggested(editor, variants, place)

  override def isAddUnambiguous: Boolean = false

  override def getText: LocalizeValue = LocalizeValue.of(textString)

  private def textString: String = elements match {
    case Seq(element) => ScalaBundle.message("import.with", element.qualifiedName)
    case _            => ScalaBundle.message("import.something")
  }

  def getFamilyName: String = ScalaBundle.message("import.compiler.suggestion")
}

object ImportCBHSuggestionFix {
  def apply(variants: Seq[CBHSuggestionToImport], place: PsiElement) = new ImportCBHSuggestionFix(variants, place)
}