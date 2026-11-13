package org.jetbrains.plugins.scala.editor.enterHandler

import consulo.language.editor.action.EnterHandlerDelegate.Result
import consulo.language.editor.action.EnterHandlerDelegateAdapter
import consulo.dataContext.DataContext
import consulo.codeEditor.action.EditorActionHandler
import consulo.document.Document
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.util.lang.ref.SimpleReference
import consulo.document.util.TextRange
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.editor.{DocumentExt, indentKeyword}
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScCatchBlock, ScFinallyBlock, ScIf}

final class FormatKeywordAfterEnterHandler extends EnterHandlerDelegateAdapter {
  type KeywordF = PsiFile => (Document, Project, PsiElement, Int) => Unit

  private val keywordsF: Map[String, KeywordF] = Map(
    ("else", indentKeyword[ScIf](ScalaTokenTypes.kELSE, _)),
    ("atch", indentKeyword[ScCatchBlock](ScalaTokenTypes.kCATCH, _)),
    ("ally", indentKeyword[ScFinallyBlock](ScalaTokenTypes.kFINALLY, _)),
  )

  private var selectedKeywordF: Option[KeywordF] = None

  override def preprocessEnter(file: PsiFile, editor: Editor,
                               caretOffset: SimpleReference[Integer], caretAdvance: SimpleReference[Integer],
                               dataContext: DataContext,
                               originalHandler: EditorActionHandler): Result = {
    val offset = caretOffset.get()

    val textBeforeCaret =
      if (offset > 4) Some(editor.getDocument.getText(TextRange.from(offset - 4, 4)))
      else None

    selectedKeywordF = textBeforeCaret.flatMap(keywordsF.get)

    Result.Continue
  }

  override def postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): Result = {

    selectedKeywordF.foreach { keywordF =>
      val project = file.getProject
      val document = editor.getDocument
      document.commit(project)
      val caretOffset = editor.getCaretModel.getOffset
      val element = file.findElementAt(caretOffset)

      if (element != null) {
        keywordF(file)(document, project, element, caretOffset)
      }
    }

    Result.Continue
  }
}
