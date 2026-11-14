package org.jetbrains.plugins.scala
package codeInsight
package intention
package booleans

import consulo.language.editor.intention.PsiElementBaseIntentionAction
import consulo.codeEditor.Editor
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElement
import consulo.language.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScInfixExpr

final class NegateComparisonIntention extends PsiElementBaseIntentionAction with DumbAware {

  import NegateComparisonIntention._

  override def isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean = {
    val infixExpr: ScInfixExpr = PsiTreeUtil.getParentOfType(element, classOf[ScInfixExpr], false)
    if (infixExpr == null) return false

    val operation = infixExpr.operation
    val refName = operation.refName
    Replacement.get(refName) match {
      case Some(replacement) if caretIsInRange(operation)(using editor) =>
        setText(ScalaCodeInsightBundle.message("negate.operation.to.inverse", refName, replacement))
        true
      case _ => false
    }
  }

  override def invoke(project: Project, editor: Editor, element: PsiElement): Unit = {
    val infixExpr: ScInfixExpr = PsiTreeUtil.getParentOfType(element, classOf[ScInfixExpr], false)
    if (infixExpr == null || !infixExpr.isValid) return

    val ScInfixExpr(ElementText(baseText), operation, ElementText(argumentText)) = infixExpr: @unchecked
    val text = s"$baseText ${Replacement(operation.refName)} $argumentText"
    negateAndValidateExpression(infixExpr, text)(using project, editor)
  }

  override def getFamilyName: String = ScalaCodeInsightBundle.message("family.name.negate.comparison")
}

object NegateComparisonIntention {
  private val Replacement = Map(
    "==" -> "!=",
    "!=" -> "==",
    ">" -> "<=",
    "<" -> ">=",
    ">=" -> "<",
    "<=" -> ">"
  )
}
