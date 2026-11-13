package org.jetbrains.plugins.scala.overrideImplement

import consulo.annotation.component.ExtensionImpl

import consulo.language.Language
import consulo.language.editor.generation.ImplementMethodHandler
import org.jetbrains.plugins.scala.api.ScalaLanguage
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.ScalaCodeInsightActionHandler
import org.jetbrains.plugins.scala.overrideImplement.ScalaOIUtil.invokeOverrideImplement

/**
 * Handler for "Override methods" action (Ctrl + I)
 */
@ExtensionImpl
class ScalaImplementMethodsHandler extends ScalaCodeInsightActionHandler with ImplementMethodHandler {
  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def startInWriteAction: Boolean = false

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit =
    invokeOverrideImplement(file, isImplement = true)(using project, editor)
}