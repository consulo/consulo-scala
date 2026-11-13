package org.jetbrains.plugins.scala.editor

import consulo.language.Language
import consulo.codeEditor.Editor
import consulo.codeEditor.EditorEx
import consulo.project.Project
import consulo.language.codeStyle.lineIndent.LineIndentProvider
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes

final class ScalaLineIndentProvider extends LineIndentProvider {

  override def isSuitableFor(language: Language): Boolean =
    language != null && language.isKindOf(ScalaLanguage.INSTANCE)

  override def getLineIndent(project: Project, editor: Editor, language: Language, offset: Int): String = {
    if (offset < 0 || editor.getDocument.getTextLength <= offset)
      return null

    val editorEx = editor match {
      case e: EditorEx => e
      case _ => return null
    }

    val highlighterIterator = editorEx.getHighlighter.createIterator(offset)
    highlighterIterator.getTokenType match {
      case ScalaTokenTypes.tMULTILINE_STRING |
           ScalaTokenTypes.tINTERPOLATED_MULTILINE_STRING |
           ScalaTokenTypes.tINTERPOLATED_STRING_END |
           ScalaTokenTypes.tINTERPOLATED_STRING_ESCAPE |
           ScalaTokenTypes.tINTERPOLATED_STRING_INJECTION =>
        LineIndentProvider.DO_NOT_ADJUST
      case _ =>
        null
    }
  }
}
