package org.jetbrains.plugins.scala.conversion.copy

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.CodeInsightSettings
import consulo.codeEditor.Editor
import consulo.document.RangeMarker
import consulo.project.{DumbService, Project}
import consulo.ui.ex.awt.DialogWrapper
import consulo.util.lang.ref.SimpleReference
import consulo.document.util.TextRange
import consulo.language.psi.{PsiDocumentManager, PsiFile}
import org.jetbrains.plugins.scala.editor.copy.Scala3IndentationBasedSyntaxCopyPastePreProcessor.AfterIndentOffsetAdjuster
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.refactoring._
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.plugins.scala.settings._
import org.jetbrains.plugins.scala.compilersettings.settings._

@ExtensionImpl
private[copy]
class ScalaCopyPastePostProcessor extends SingularCopyPastePostProcessor[AssociationsTransferableData](AssociationsTransferableData.flavor) {

  override def collectTransferableData(startOffsets: Array[Int], endOffsets: Array[Int])
                                      (implicit file: PsiFile,
                                       editor: Editor): Option[AssociationsTransferableData] = {
    if (DumbService.getInstance(file.getProject).isDumb)
      return None

    if (startOffsets.isEmpty)
      return None
    val scalaFile = file match {
      case sf: ScalaFile  => sf
      case _ =>
        return None
    }

    // Don't even try to collect imports information when the feature is disabled to save CPU resources
    // (the similar thing is done in com.intellij.codeInsight.editorActions.AbstractJavaCopyPasteReferenceProcessor)
    if (ScalaApplicationSettings.getInstance.ADD_IMPORTS_ON_PASTE == CodeInsightSettings.NO)
      return None

    val ranges = startOffsets.zip(endOffsets).map {
      case (startOffset, endOffset) => TextRange.create(startOffset, endOffset)
    }
    Option(Associations.collectAssociations(ranges.toSeq*)(using scalaFile)).map(AssociationsTransferableData(_))
  }

  override def processTransferableData(
    bounds: RangeMarker,
    caretOffset: Int,
    ref: SimpleReference[java.lang.Boolean],
    associations: AssociationsTransferableData
  )(implicit project: Project, editor: Editor, file: ScalaFile): Unit = {
    import CodeInsightSettings._
    if (DumbService.getInstance(project).isDumb)
      return

    val setting = ScalaApplicationSettings.getInstance.ADD_IMPORTS_ON_PASTE
    if (setting == NO)
      return

    PsiDocumentManager.getInstance(project).commitAllDocuments()
    val adjuster = AfterIndentOffsetAdjuster.getAndClearFromUserdata(file)
    associations.toAssociations.restoreOnUiThread(bounds, adjuster) {
      case bindings if setting == ASK =>
        val bindingsSorted = bindings.filterNot(_.path.isEmpty).sortBy(_.path)
        val bindingsUnique = bindingsSorted.distinctBy(b => (b.path, b.aliasName))
        if (bindingsUnique.nonEmpty) {
          val dialog = new RestoreReferencesDialog(project, bindingsUnique, file.features, editor.getColorsScheme)
          dialog.show()
          dialog.getExitCode match {
            case DialogWrapper.OK_EXIT_CODE =>
              val selectedElements = dialog.getSelectedElements
              bindingsUnique.filter(selectedElements.contains)
            case _ => Seq.empty
          }
        }
        else Seq.empty
      case bindings =>
        bindings
    }
  }
}
