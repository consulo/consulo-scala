package org.jetbrains.plugins.scala
package codeInsight
package intention
package controlFlow

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.{PsiDocumentManager, PsiElement}
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createElementFromText
import org.jetbrains.plugins.scala.project.{ProjectContext, ScalaFeatures}

import scala.collection.mutable

final class MergeElseIfIntention extends PsiElementBaseIntentionAction with DumbAware {
  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    val ifStmt: ScIf = PsiTreeUtil.getParentOfType(element, classOf[ScIf], false)
    if (ifStmt == null) return false

    val offset = editor.getCaretModel.getOffset
    val thenBranch = ifStmt.thenExpression.orNull
    val elseBranch = ifStmt.elseExpression.orNull
    if (thenBranch == null || elseBranch == null) return false

    if (!(thenBranch.getTextRange.getEndOffset <= offset && offset <= elseBranch.getTextRange.getStartOffset))
      return false

    val blockExpr = ifStmt.elseExpression.orNull
    if (blockExpr != null && blockExpr.is[ScBlockExpr]) {
      val exprs = blockExpr.asInstanceOf[ScBlockExpr].exprs
      if (exprs.size == 1 && exprs.head.is[ScIf]) {
        return true
      }
    }

    false
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val ifStmt: ScIf = PsiTreeUtil.getParentOfType(element, classOf[ScIf], false)
    if (ifStmt == null || !ifStmt.isValid) return

    val elseOffset = ifStmt.elseKeyword.get.getTextOffset
    val caretOffset = editor.getCaretModel.getOffset

    val newlineBeforeElse = ifStmt.elseKeyword.exists(_.startsFromNewLine(ignoreComments = false))
    val expr = new mutable.StringBuilder()
      .append("if (").append(ifStmt.condition.get.getText).append(") ")
      .append(ifStmt.thenExpression.get.getText)
      .append(if (newlineBeforeElse) "\n" else " ")
      .append("else ")
      .append(ifStmt.elseExpression.get.getText.trim.drop(1).dropRight(1))

    implicit val ctx: ProjectContext = element.getManager
    implicit val features: ScalaFeatures = element
    val newIfStmt = ScalaPsiUtil.convertIfToBracelessIfNeeded(createElementFromText[ScIf](expr.toString(), features), recursive = true)
    val newElseOffset = ifStmt.getTextOffset + newIfStmt.elseKeyword.get.getStartOffsetInParent

    IntentionPreviewUtils.write { () =>
      ifStmt.replaceExpression(newIfStmt, removeParenthesis = true)
      editor.getCaretModel.moveToOffset(newElseOffset - elseOffset + caretOffset)
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument)
    }
  }

  override def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.merge.else.if")

  override def getText: String = ScalaCodeInsightBundle.message("merge.elseif")
}
