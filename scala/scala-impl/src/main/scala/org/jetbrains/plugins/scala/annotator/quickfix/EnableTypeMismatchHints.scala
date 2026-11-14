package org.jetbrains.plugins.scala.annotator.quickfix

import consulo.language.editor.intention.SyntheticIntentionAction
import consulo.localize.LocalizeValue
import consulo.language.editor.intention.IntentionAction
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.annotator.TypeMismatchHints
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.impl.source.ScalaCodeFragment
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

private[annotator] object EnableTypeMismatchHints extends SyntheticIntentionAction {

  def getFamilyName: String = ScalaBundle.message("family.name.enable.type.mismatch.hints")

  override def getText: LocalizeValue = LocalizeValue.of(getFamilyName)

  override def startInWriteAction = false

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean =
    !file.is[ScalaCodeFragment] && !ScalaProjectSettings.in(project).isTypeMismatchHints

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit = {
    ScalaProjectSettings.in(project).setTypeMismatchHints(true)
    TypeMismatchHints.refreshIn(project)
  }
}