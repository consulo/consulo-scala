package org.jetbrains.plugins.scala.codeInsight.intention.expression

import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.document.util.TextRange
import consulo.language.psi.PsiElement
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInspection.quickfix.ConvertFromInfixExpressionQuickFix
import org.jetbrains.plugins.scala.lang.psi.api.expr._

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ConvertFromInfixExpressionIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Expressions"))
class ConvertFromInfixExpressionIntention extends PsiElementBaseIntentionAction with DumbAware {
  override def getFamilyName: String = ScalaBundle.message("family.name.convert.from.infix.expression")

  override def getText: String = getFamilyName

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    val infixExpr = PsiTreeUtil.getParentOfType(element, classOf[ScInfixExpr], false)
    if (infixExpr == null) return false
    val range: TextRange = infixExpr.operation.nameId.getTextRange
    val offset = editor.getCaretModel.getOffset
    range.getStartOffset <= offset && offset <= range.getEndOffset
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val infixExpr = PsiTreeUtil.getParentOfType(element, classOf[ScInfixExpr], false)
    if (infixExpr == null || !infixExpr.isValid) return

    val quickFix = new ConvertFromInfixExpressionQuickFix(infixExpr)
    quickFix.invoke(project, editor, element.getContainingFile)
  }
}
