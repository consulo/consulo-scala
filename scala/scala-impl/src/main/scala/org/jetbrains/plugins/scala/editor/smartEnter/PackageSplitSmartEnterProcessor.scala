package org.jetbrains.plugins.scala.editor.smartEnter

import consulo.language.editor.action.SmartEnterProcessor
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.editor.enterHandler.PackageSplitEnterHandler

class PackageSplitSmartEnterProcessor extends SmartEnterProcessor {
  override def process(project: Project, editor: Editor, psiFile: PsiFile): Boolean = {
    val document = editor.getDocument
    val offset = editor.getCaretModel.getOffset

    PackageSplitEnterHandler.process(document, offset, processAtAnyChar = true, addExtraNewLine = true) match {
      case PackageSplitEnterHandler.Processed(caretOffsetNew: Int, caretShift: Int) =>
        editor.getCaretModel.moveToOffset(caretOffsetNew + caretShift)
        true
      case _ =>
        false
    }
  }
}