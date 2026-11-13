package org.jetbrains.plugins.scala.lang.surroundWith

import consulo.codeEditor.Editor
import consulo.document.util.TextRange
import consulo.language.editor.surroundWith.Surrounder
import consulo.language.psi.PsiElement
import consulo.project.Project

/**
 * Consulo replacement of IntelliJ `ModCommandSurrounder`: surrounds the original PSI in a write action.
 */
abstract class ScalaPsiSurrounder extends Surrounder {
  override final def surroundElements(project: Project, editor: Editor, elements: Array[PsiElement]): TextRange =
    if (elements.isEmpty) null
    else surroundElements(elements, editor).orNull

  protected def surroundElements(elements: Array[PsiElement], editor: Editor): Option[TextRange]
}
