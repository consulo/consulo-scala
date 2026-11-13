package org.jetbrains.plugins.scala.codeInspection.parameters

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.intention.HighPriorityAction
import consulo.language.editor.inspection.{LocalInspectionTool, LocalQuickFix, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.parameters.TypedParameterWithoutParenthesisInspection._
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameterClause
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory

@ExtensionImpl
final class TypedParameterWithoutParenthesisInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("typed.parameter.without.parenthesis.in.function.literal"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.method.signature"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.INFO

  override def getShortName: String = "TypedParameterWithoutParen"

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case parameterClause: ScParameterClause if !parameterClause.hasParenthesis && parameterClause.parameters.sizeIs == 1 =>
      for {
        param <- parameterClause.parameters
        _     <- param.typeElement
      } holder.registerProblem(parameterClause, getDisplayName.get, createQuickFix(parameterClause))
    case _ =>
  }
}

object TypedParameterWithoutParenthesisInspection {
  private def createQuickFix(pc: ScParameterClause): LocalQuickFix = new AbstractFixOnPsiElement(ScalaInspectionBundle.message("surround.with.parenthesis"), pc)
    with HighPriorityAction
    with DumbAware {
    override protected def doApplyFix(pclause: ScParameterClause)
                                     (implicit project: Project): Unit = {
      val replacement = ScalaPsiElementFactory.createExpressionFromText("(" + pclause.getText + ")", pclause)
      pclause.replace(replacement)
    }
  }
}
