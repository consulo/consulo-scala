package org.jetbrains.plugins.scala.codeInspection.varCouldBeValInspection

import consulo.language.editor.intention.SyntheticIntentionAction
import consulo.localize.LocalizeValue
import consulo.language.editor.inspection.FileModifier
import consulo.language.editor.intention.IntentionAction
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValue
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createVarFromValDeclaration

final class ValToVarQuickFix(value: ScValue) extends SyntheticIntentionAction {

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean =
    file.is[ScalaFile]

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit = {
    val replacement = createVarFromValDeclaration(value)
    value.replace(replacement)
  }

  override def getText: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("convert.val.to.var"))

  def getFamilyName: String = getText.get

  override def startInWriteAction: Boolean = true

  override def getFileModifierForPreview(target: PsiFile): FileModifier =
    new ValToVarQuickFix(PsiTreeUtil.findSameElementInCopy(value, target))
}
