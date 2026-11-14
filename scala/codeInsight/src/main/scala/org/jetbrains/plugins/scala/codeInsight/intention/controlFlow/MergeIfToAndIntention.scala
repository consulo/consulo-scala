package org.jetbrains.plugins.scala
package codeInsight
package intention
package controlFlow

import consulo.localize.LocalizeValue
import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

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

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.MergeIfToAndIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Control flow"))
final class MergeIfToAndIntention extends PsiElementBaseIntentionAction with DumbAware {

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    val ifStmt: ScIf = PsiTreeUtil.getParentOfType(element, classOf[ScIf], false)
    if (ifStmt == null) return false

    val offset = editor.getCaretModel.getOffset
    val thenBranch = ifStmt.thenExpression.orNull
    val elseBranch = ifStmt.elseExpression.orNull
    if (thenBranch == null || elseBranch != null) return false

    val condition = ifStmt.condition.orNull
    if (condition == null) return false

    if (!(ifStmt.getTextRange.getStartOffset <= offset &&
      offset <= condition.getTextRange.getStartOffset - 1)) return false

    thenBranch match {
      case branch: ScBlockExpr =>
        val exprs = branch.exprs
        if (exprs.size != 1 || !exprs.head.is[ScIf]) return false

        val innerIfStmt = exprs.head.asInstanceOf[ScIf]
        val innerElseBranch = innerIfStmt.elseExpression.orNull
        if (innerElseBranch != null) return false
        true

      case branch: ScIf =>
        val innerElseBranch = branch.elseExpression.orNull
        if (innerElseBranch != null) return false
        true

      case _ => false
    }
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val ifStmt: ScIf = PsiTreeUtil.getParentOfType(element, classOf[ScIf], false)
    if (ifStmt == null || !ifStmt.isValid) return

    val outerCondition = ifStmt.condition.get.getText
    val innerIfStmt = ifStmt.thenExpression.get match {
      case c: ScBlockExpr => c.exprs.head.asInstanceOf[ScIf]
      case c: ScIf => c
    }
    val innerThenBranch = innerIfStmt.thenExpression.get
    val innerCondition = innerIfStmt.condition.get.getText

    val expr = new mutable.StringBuilder()
      .append("if (").append(outerCondition).append(" && ").append(innerCondition).append(") ")
      .append(innerThenBranch.getText)

    IntentionPreviewUtils.write { () =>
      implicit val ctx: ProjectContext = project
      implicit val features: ScalaFeatures = element
      val newIf = ScalaPsiUtil.convertIfToBracelessIfNeeded(createElementFromText[ScIf](expr.toString(), element), recursive = true)
      ifStmt.replaceExpression(newIf, removeParenthesis = true)
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument)
    }
  }

  def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.merge.nested.ifs.to.anded.condition")

  override def getText: LocalizeValue = LocalizeValue.of(ScalaCodeInsightBundle.message("merge.nested.ifs"))
}
