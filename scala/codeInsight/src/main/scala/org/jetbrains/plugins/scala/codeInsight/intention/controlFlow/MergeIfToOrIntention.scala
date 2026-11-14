package org.jetbrains.plugins.scala
package codeInsight
package intention
package controlFlow

import consulo.localize.LocalizeValue
import consulo.annotation.component.ExtensionImpl
import consulo.language.editor.intention.IntentionMetaData

import consulo.language.editor.PsiEquivalenceUtil
import consulo.language.editor.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.util.PsiTreeUtil
import consulo.language.psi.{PsiDocumentManager, PsiElement}
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createElementFromText
import org.jetbrains.plugins.scala.project.{ProjectContext, ScalaFeatures}

import scala.collection.mutable

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.MergeIfToOrIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Control flow"))
final class MergeIfToOrIntention extends PsiElementBaseIntentionAction with DumbAware {

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    val ifStmt: ScIf = PsiTreeUtil.getParentOfType(element, classOf[ScIf], false)
    if (ifStmt == null) return false

    val offset = editor.getCaretModel.getOffset
    val thenBranch = ifStmt.thenExpression.orNull
    val elseBranch = ifStmt.elseExpression.orNull
    if (thenBranch == null || elseBranch == null) return false

    if (!elseBranch.is[ScIf]) return false
    if (ifStmt.condition.orNull == null) return false

    if (!(thenBranch.getTextRange.getEndOffset <= offset && offset <= elseBranch.getTextRange.getStartOffset) &&
      !(ifStmt.getTextRange.getStartOffset <= offset && offset <= ifStmt.condition.get.getTextRange.getStartOffset))
      return false

    val innerThenBranch = elseBranch.asInstanceOf[ScIf].thenExpression.orNull
    if (innerThenBranch == null) return false

    PsiEquivalenceUtil.areElementsEquivalent(thenBranch, innerThenBranch)
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val ifStmt: ScIf = PsiTreeUtil.getParentOfType(element, classOf[ScIf], false)
    if (ifStmt == null || !ifStmt.isValid) return

    val start = ifStmt.getTextRange.getStartOffset
    val outerCondition = ifStmt.condition.get.getText
    val innerIfStmt = ifStmt.elseExpression.get.asInstanceOf[ScIf]
    val innerCondition = innerIfStmt.condition.get.getText
    val innerElseBranch = innerIfStmt.elseExpression.orNull
    val newlineBeforeElse = ifStmt.children.find(_.getNode.getElementType == ScalaTokenTypes.kELSE)
      .exists(_.getPrevSibling.getText.contains("\n"))

    val expr = new mutable.StringBuilder()
      .append("if (").append(outerCondition).append(" || ").append(innerCondition).append(") ")
      .append(ifStmt.thenExpression.get.getText)
    if (innerElseBranch != null)
      expr.append(if (newlineBeforeElse) "\n" else " ")
        .append("else ").append(innerElseBranch.getText)

    implicit val ctx: ProjectContext = project
    implicit val features: ScalaFeatures = element
    val newIfStmt = ScalaPsiUtil.convertIfToBracelessIfNeeded(createElementFromText[ScIf](expr.toString(), element), recursive = true)

    IntentionPreviewUtils.write { () =>
      ifStmt.replaceExpression(newIfStmt, removeParenthesis = true)
      editor.getCaretModel.moveToOffset(start)
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument)
    }
  }

  def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.merge.equivalent.ifs.to.ored.condition")

  override def getText: LocalizeValue = LocalizeValue.of(ScalaCodeInsightBundle.message("merge.sequential.ifs"))
}
