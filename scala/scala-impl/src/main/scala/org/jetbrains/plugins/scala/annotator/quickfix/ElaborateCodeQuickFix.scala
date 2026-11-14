package org.jetbrains.plugins.scala.annotator.quickfix

import consulo.codeEditor.Editor
import consulo.language.editor.intention.SyntheticIntentionAction
import consulo.language.psi.PsiFile
import consulo.localize.LocalizeValue
import consulo.project.Project
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.impl.source.ScalaCodeFragment
import org.jetbrains.plugins.scala.semantic.ElaborateCodeAction

object ElaborateCodeQuickFix extends SyntheticIntentionAction {
  override def getText: LocalizeValue = LocalizeValue.of(ScalaBundle.message("elaborate.code.quick.fix"))

  override def startInWriteAction: Boolean = false

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean =
    !file.is[ScalaCodeFragment]

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit = {
    new ElaborateCodeAction().perform(project, editor, file.asInstanceOf[ScalaFile])
  }
}
