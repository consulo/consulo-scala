package org.jetbrains.plugins.scala.codeInspection.postfix

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.project.Project
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, childOf}
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

@ExtensionImpl
class PostfixMethodCallInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.use.of.postfix.method.call"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def isEnabledByDefault: Boolean = false

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case pexpr: ScPostfixExpr if !safe(pexpr) =>
      holder.registerProblem(pexpr, getDisplayName.get, new AddDotFix(pexpr))
    case _ =>
  }

  private def safe(pexpr: ScPostfixExpr): Boolean = {
    pexpr.getContext match {
      case _: ScParenthesisedExpr => true
      case _: ScArgumentExprList => true
      case (_: ScAssignment) childOf (_: ScArgumentExprList) => true //named arguments
      case _ =>
        val next = pexpr.getNextSiblingNotWhitespace
        if (next == null) return false
        val nextNode = next.getNode
        if (nextNode == null) return false
        nextNode.getElementType == ScalaTokenTypes.tSEMICOLON
    }
  }
}

class AddDotFix(pexpr: ScPostfixExpr) extends AbstractFixOnPsiElement(ScalaInspectionBundle.message("add.dot.to.method.call"), pexpr) {

  override protected def doApplyFix(postfix: ScPostfixExpr)
                                   (implicit project: Project): Unit = {
    val expr = ScalaPsiElementFactory.createEquivQualifiedReference(postfix)
    postfix.replaceExpression(expr, removeParenthesis = true)
  }
}
