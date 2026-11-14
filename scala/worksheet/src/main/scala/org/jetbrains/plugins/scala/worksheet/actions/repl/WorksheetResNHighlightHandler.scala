package org.jetbrains.plugins.scala.worksheet.actions.repl

import consulo.language.editor.highlight.usage.HighlightUsagesHandlerBase
import consulo.codeEditor.Editor
import consulo.language.psi.{PsiElement, PsiFile}
import java.util.function.Consumer

import java.{util => ju}

private final class WorksheetResNHighlightHandler(
  editor: Editor,
  file: PsiFile,
  elementAtCaret: PsiElement,
  referenced: Array[PsiElement]
) extends HighlightUsagesHandlerBase[PsiElement](editor, file) {

  override def selectTargets(
    targets: ju.List[PsiElement],
    selectionConsumer: Consumer[ju.List[PsiElement]]
  ): Unit = {
    selectionConsumer.accept(targets)
  }

  override def getTargets: ju.List[PsiElement] = {
    val list = new ju.ArrayList[PsiElement]
    list.add(elementAtCaret)
    referenced.foreach(list.add)
    list
  }

  override def computeUsages(targets: ju.List[PsiElement]): Unit = {
    if (targets.size >= 2) {
      myReadUsages.add(targets.get(0).getTextRange)
      targets.stream().skip(1).forEach { target =>
        myWriteUsages.add(target.getTextRange)
      }
    }
  }
}
