package org.jetbrains.plugins.scala
package codeInsight
package intention
package controlFlow

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
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScIf, ScInfixExpr, ScParenthesisedExpr}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createElementFromText
import org.jetbrains.plugins.scala.project.ScalaFeatures

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.SplitIfIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Control flow"))
final class SplitIfIntention extends PsiElementBaseIntentionAction with DumbAware {

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean =
    element.parentOfType(classOf[ScIf], strict = false).exists {
      case ScIf(Some(ScInfixExpr(_, operation, _)), _, _) if caretIsInRange(operation)(using editor) =>
        operation.refName == "&&"
      case _ => false
    }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val ifStmt = PsiTreeUtil.getParentOfType(element, classOf[ScIf], false)
    if (ifStmt == null || !ifStmt.isValid) return

    val ScIf(Some(infix: ScInfixExpr), Some(ElementText(thenBranchText)), maybeElseBranch) = ifStmt: @unchecked
    val ScInfixExpr.withAssoc(base, _, argument) = infix

    def conditionText(e: ScExpression): String = (e match {
      case ScParenthesisedExpr(expression) => expression
      case expression => expression
    }).getText.trim

    val prefix =
      s"""if (${conditionText(base)})
         |if (${conditionText(argument)}) $thenBranchText""".stripMargin

    val suffix = maybeElseBranch match {
      case Some(ElementText(text)) =>
        val separator = if (prefix.trim.endsWith("}")) ' ' else '\n'
        s"""${separator}else $text
           |else $text""".stripMargin
      case _ => ""
    }

    import ifStmt.projectContext
    implicit val features: ScalaFeatures = element
    val start = ifStmt.getTextRange.getStartOffset
    val newIfStmt = ScalaPsiUtil.convertIfToBracelessIfNeeded(createElementFromText[ScIf](prefix + suffix, element), recursive = true)
    val diff = newIfStmt.condition.get.getTextRange.getStartOffset - newIfStmt.getTextRange.getStartOffset

    IntentionPreviewUtils.write { () =>
      ifStmt.replaceExpression(newIfStmt, removeParenthesis = true)
      editor.getCaretModel.moveToOffset(start + diff)
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument)
    }
  }

  override def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.split.if")

  override def getText: String = ScalaCodeInsightBundle.message("split.into.2.if.s")
}
