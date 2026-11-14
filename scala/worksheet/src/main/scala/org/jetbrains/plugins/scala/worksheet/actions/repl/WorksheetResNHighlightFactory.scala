package org.jetbrains.plugins.scala.worksheet.actions.repl

import consulo.annotation.component.ExtensionImpl

import consulo.language.editor.TargetElementUtil
import consulo.language.editor.highlight.usage.{HighlightUsagesHandlerBase, HighlightUsagesHandlerFactory}
import consulo.codeEditor.Editor
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.worksheet.WorksheetFile

@ExtensionImpl(order = "before ScalaHighlightUsagesHandlerFactory")
final class WorksheetResNHighlightFactory extends HighlightUsagesHandlerFactory[PsiElement] {

  override def createHighlightUsagesHandler(editor: Editor, file: PsiFile): HighlightUsagesHandlerBase[PsiElement] =
    file match {
      case file: WorksheetFile if ResNUtils.isResNSupportedInFile(file) =>
        doCreateHighlightUsagesHandler(editor, file)
      case _ =>
        null
    }

  private def doCreateHighlightUsagesHandler(editor: Editor, file: PsiFile): WorksheetResNHighlightHandler = {
    val offset = TargetElementUtil.adjustOffset(file, editor.getDocument, editor.getCaretModel.getOffset)
    val elementAtCaret = file.findElementAt(offset)

    if (elementAtCaret != null && isResNReference(elementAtCaret)) {
      val referenced = WorksheetResNGotoHandler.findReferencedPsi(elementAtCaret.getParent)
      val highlighter = referenced.map(new WorksheetResNHighlightHandler(editor, file, elementAtCaret, _))
      highlighter.orNull
    } else {
      null
    }
  }


  @inline
  private def isResNReference(psi: PsiElement): Boolean = {
    val text = psi.getText
    ResNUtils.ResNRegex.matches(text) && psi.getParent.isInstanceOf[ScReferenceExpression]
  }
}
