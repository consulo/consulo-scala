package org.jetbrains.plugins.scala.editor

import consulo.language.Language
import consulo.document.Document
import consulo.project.Project
import consulo.language.codeStyle.lineIndent.{LineIndentProvider, SemanticEditorPositionFactory}
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes

final class ScalaLineIndentProvider extends LineIndentProvider {

  override def isSuitableFor(language: Language): Boolean =
    language != null && language.isKindOf(ScalaLanguage.INSTANCE)

  // upstream: getLineIndent(project, editor, language, offset) read the token type from the editor highlighter
  override def getLineIndent(project: Project, document: Document, positionFactory: SemanticEditorPositionFactory, language: Language, offset: Int): String = {
    if (offset < 0 || document.getTextLength <= offset)
      return null

    val position = positionFactory.create(offset, _ => null)
    val isInString = Seq(
      ScalaTokenTypes.tMULTILINE_STRING,
      ScalaTokenTypes.tINTERPOLATED_MULTILINE_STRING,
      ScalaTokenTypes.tINTERPOLATED_STRING_END,
      ScalaTokenTypes.tINTERPOLATED_STRING_ESCAPE,
      ScalaTokenTypes.tINTERPOLATED_STRING_INJECTION
    ).exists(position.isAt)

    if (isInString) LineIndentProvider.DO_NOT_ADJUST
    else null
  }
}
