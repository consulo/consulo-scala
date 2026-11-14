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
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScWhile
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionFromText

@ExtensionImpl
@IntentionMetaData(ignoreId = "scala.ReplaceWhileWithDoWhileIntention", fileExtensions = Array("scala"), categories = Array("Scala", "Control flow"))
final class ReplaceWhileWithDoWhileIntention extends PsiElementBaseIntentionAction with DumbAware {

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    if (element.isInScala3File) return false

    val result =
      for {
        whileStmt <- Option(PsiTreeUtil.getParentOfType(element, classOf[ScWhile], false))
        condition <- whileStmt.condition
        _ <- whileStmt.expression
      } yield {
        val offset = editor.getCaretModel.getOffset
        offset >= whileStmt.getTextRange.getStartOffset && offset < condition.getTextRange.getStartOffset
      }

    result.contains(true)
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val whileStmt: ScWhile = PsiTreeUtil.getParentOfType(element, classOf[ScWhile])
    if (whileStmt == null || !whileStmt.isValid) return

    for {
      condition <- whileStmt.condition
      body <- whileStmt.expression
    } {
      val condText = condition.getText
      val bodyText = body.getText

      val newStmtText = s"if ($condText) {\n do $bodyText while ($condText)\n}"

      val newStmt = createExpressionFromText(newStmtText, element)(using element.getManager)

      IntentionPreviewUtils.write { () =>
        whileStmt.replaceExpression(newStmt, removeParenthesis = true)
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument)
      }
    }
  }

  def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.replace.while.with.do.while")

  override def getText: LocalizeValue = LocalizeValue.of(getFamilyName)
}
