package org.jetbrains.plugins.scala

import consulo.language.editor.action.LanguageCodeInsightActionHandler
import consulo.codeEditor.Editor
import consulo.language.psi.PsiFile

trait ScalaCodeInsightActionHandler extends LanguageCodeInsightActionHandler {

  override def isValidFor(editor: Editor, file: PsiFile): Boolean =
    editor != null &&
      file != null &&
      file.getLanguage.isKindOf(ScalaLanguage.INSTANCE)
}
