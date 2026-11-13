package org.jetbrains.plugins.scala.overrideImplement

import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.ScalaCodeInsightActionHandler
import org.jetbrains.plugins.scala.overrideImplement.ScalaOIUtil.invokeOverrideImplement

/**
 * Handler for "Override methods" action (Ctrl + I)
 */
class ScalaImplementMethodsHandler extends ScalaCodeInsightActionHandler {
  override def startInWriteAction: Boolean = false

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit =
    invokeOverrideImplement(file, isImplement = true)(using project, editor)
}