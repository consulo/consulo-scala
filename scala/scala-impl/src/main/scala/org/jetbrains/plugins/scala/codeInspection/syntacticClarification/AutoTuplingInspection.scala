package org.jetbrains.plugins.scala.codeInspection.syntacticClarification

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.project.Project
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{MethodInvocation, ScMethodCall, ScReferenceExpression}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionFromText

@ExtensionImpl
class AutoTuplingInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.auto.tupling"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.syntactic.clarification"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def isEnabledByDefault: Boolean = false

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case mc @ ScMethodCall(ref: ScReferenceExpression, _) if ref.bind().exists(_.tuplingUsed) =>
      holder.registerProblem(mc.args, ScalaInspectionBundle.message("scala.compiler.will.replace.this.argument.list.with.tuple"), new MakeTuplesExplicitFix(mc))
    case _ =>
  }
}

class MakeTuplesExplicitFix(invoc: MethodInvocation) extends AbstractFixOnPsiElement(ScalaInspectionBundle.message("make.tuple.explicit"), invoc) {

  override protected def doApplyFix(element: MethodInvocation)
                                   (implicit project: Project): Unit = element match {
    case mc: ScMethodCall =>
      val newArgsText = s"(${mc.args.getText})"
      val invokedExprText = mc.getInvokedExpr.getText
      val newCall = createExpressionFromText(s"$invokedExprText$newArgsText", mc)
      mc.replaceExpression(newCall, removeParenthesis = false)
    case _ =>

  }
}
