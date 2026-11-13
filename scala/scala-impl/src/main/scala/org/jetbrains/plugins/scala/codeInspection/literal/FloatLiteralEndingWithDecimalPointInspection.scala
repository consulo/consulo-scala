package org.jetbrains.plugins.scala.codeInspection.literal

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionFromText

@ExtensionImpl
final class FloatLiteralEndingWithDecimalPointInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.floating.point.literal.ending.with.dot"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case lit: ScLiteral if lit.getText.endsWith(".") =>
      holder.registerProblem(lit, getDisplayName.get, new MakeDoubleFix(lit), new MakeFloatFix(lit), new AddZeroAfterDecimalPoint(lit))
    case _ =>
  }
}

final class MakeDoubleFix(lit: ScLiteral)
  extends AbstractFixOnPsiElement(ScalaInspectionBundle.message("convert.to.floating.point", lit.getText.dropRight(1) + "d"), lit)
    with DumbAware {
  override protected def doApplyFix(l: ScLiteral)
                                   (implicit project: Project): Unit =
    l.replace(createExpressionFromText(l.getText.dropRight(1) + "d", l))
}

final class MakeFloatFix(lit: ScLiteral)
  extends AbstractFixOnPsiElement(ScalaInspectionBundle.message("convert.to.floating.point", lit.getText.dropRight(1) + "f"), lit)
    with DumbAware {
  override protected def doApplyFix(l: ScLiteral)
                                   (implicit project: Project): Unit =
    l.replace(createExpressionFromText(l.getText.dropRight(1) + "f", l))
}

final class AddZeroAfterDecimalPoint(lit: ScLiteral)
  extends AbstractFixOnPsiElement(ScalaInspectionBundle.message("convert.to.floating.point", lit.getText + "0"), lit)
    with DumbAware {
  override protected def doApplyFix(l: ScLiteral)
                                   (implicit project: Project): Unit =
    l.replace(createExpressionFromText(l.getText + "0", l))
}
