package org.jetbrains.plugins.scala.annotator.quickfix

import consulo.localize.LocalizeValue
import consulo.language.editor.inspection.FileModifier
import consulo.language.editor.intention.IntentionAction
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.PsiFile
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInsight.intention.types.AddOnlyStrategy
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.types.ScType

final class AddReturnTypeFix(fun: ScFunctionDefinition, tp: ScType) extends IntentionAction {
  override def getText: LocalizeValue = LocalizeValue.of(ScalaBundle.message("add.return.type"))

  def getFamilyName: String = getText.get

  override def invoke(project: Project, editor: Editor, file: PsiFile): Unit =
    new AddOnlyStrategy(Option(editor)).addTypeAnnotation(tp, fun.getParent, fun.parameterList)

  override def startInWriteAction(): Boolean = true

  override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = fun.returnTypeElement.isEmpty

  override def getFileModifierForPreview(target: PsiFile): FileModifier =
    new AddReturnTypeFix(PsiTreeUtil.findSameElementInCopy(fun, target), tp)
}
