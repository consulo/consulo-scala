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
import consulo.language.psi.{PsiDocumentManager, PsiElement, PsiManager}
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.extensions.*
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.expr.*
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createNewLine

import scala.collection.immutable.ArraySeq

final class RemoveRedundantElseIntention extends PsiElementBaseIntentionAction with DumbAware {

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    val ifStmt: ScIf = PsiTreeUtil.getParentOfType(element, classOf[ScIf], false)
    if (ifStmt == null) return false

    val thenBranch = ifStmt.thenExpression.orNull
    val elseBranch = ifStmt.elseExpression.orNull
    val condition = ifStmt.condition.orNull
    if (thenBranch == null || elseBranch == null || condition == null) return false

    val offset = editor.getCaretModel.getOffset
    if (!(thenBranch.getTextRange.getEndOffset <= offset && offset <= elseBranch.getTextRange.getStartOffset))
      return false

    thenBranch match {
      case tb: ScBlockExpr =>
        val lastExpr = tb.resultExpression.orNull
        if (lastExpr == null) return false
        if (lastExpr.is[ScReturn, ScThrow]) return true
        false
      case e: ScExpression =>
        if (e.is[ScReturn, ScThrow]) return true
        false
    }
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    for {
      ifStmt <- Option(PsiTreeUtil.getParentOfType(element, classOf[ScIf], false))
      if ifStmt.isValid
      thenBranch <- ifStmt.thenExpression
      elseKeyWord = thenBranch.getNextSiblingNotWhitespaceComment
      elseBranch <- ifStmt.elseExpression
      children = elseBranch.copy().children.to(ArraySeq)
      from <- children.find(_.getNode.getElementType != ScalaTokenTypes.tLBRACE)
      to <- children.findLast(_.getNode.getElementType != ScalaTokenTypes.tRBRACE)
    } {
      IntentionPreviewUtils.write { () =>
        elseKeyWord.delete()
        elseBranch.delete()
        val adjustedFrom = if (from.isWhitespace) from.nextSibling.getOrElse(from) else from
        ifStmt.getParent.addRangeAfter(adjustedFrom, to, ifStmt)
        ifStmt.getParent.addAfter(createNewLine()(using PsiManager.getInstance(project)), ifStmt)
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument)
      }
    }
  }

  override def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.remove.redundant.else")

  override def getText: String = ScalaCodeInsightBundle.message("remove.redundant.else")
}
