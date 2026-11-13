package org.jetbrains.plugins.scala.annotator.quickfix

import consulo.localize.LocalizeValue
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement
import consulo.codeEditor.Editor
import consulo.project.Project
import consulo.language.psi.{PsiElement, PsiFile}
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

class AddCallParenthesesQuickFix(reference: PsiElement) extends LocalQuickFixAndIntentionActionOnPsiElement(reference) {
  override def invoke(project: Project, psiFile: PsiFile, editor: Editor, startElement: PsiElement, endElement: PsiElement): Unit = {
    if (!reference.isValid) return

    val replacement = ScalaPsiElementFactory.createExpressionFromText(s"${reference.getText}()", reference)(using project)

    IntentionPreviewUtils.write { () =>
      reference.replace(replacement)
    }
  }

  override def getText: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("add.call.parentheses"))
  def getFamilyName: String = getText.get
}
