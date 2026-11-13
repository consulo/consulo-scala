package org.jetbrains.plugins.scala.autoImport.quickFix

import consulo.localize.LocalizeValue
import consulo.codeEditor.Editor
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings

class ImportExtensionMethodFix(ref: ScReferenceExpression,
                               computation: ConversionToImportComputation) extends ScalaImportElementFix[ExtensionMethodToImport](ref) {
  override protected def findElementsToImport(): Seq[ExtensionMethodToImport] = computation.extensionMethods

  override def createAddImportAction(editor: Editor): ScalaAddImportAction[?, ?] =
    ScalaAddImportAction.importExtensionMethod(editor, elements, ref)

  override def isAddUnambiguous: Boolean = false

  override def getText: LocalizeValue = LocalizeValue.of(textString)

  private def textString: String = elements match {
    case Seq(extensionMethod) => ScalaBundle.message("import.with", extensionMethod.qualifiedName)
    case _ => getFamilyName
  }

  def getFamilyName: String = ScalaBundle.message("import.extension.method")

  override def shouldShowHint(): Boolean =
    super.shouldShowHint() && ScalaApplicationSettings.getInstance().SHOW_IMPORT_POPUP_EXTENSION_METHODS
}

object ImportExtensionMethodFix {
  def apply(ref: ScReferenceExpression, computation: ConversionToImportComputation) =
    new ImportExtensionMethodFix(ref, computation)
}
