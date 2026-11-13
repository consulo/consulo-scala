package org.jetbrains.plugins.scala.codeInspection.format

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import org.jetbrains.plugins.scala.codeInspection.{PsiUpdateContext, PsiUpdateFix}
import consulo.language.editor.inspection.{LocalInspectionTool, LocalQuickFix, ProblemsHolder}
import consulo.language.psi.{PsiElement, PsiElementVisitor}
import org.jetbrains.plugins.scala.codeInspection.format.LegacyStringFormattingInspection._
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.format._
import org.jetbrains.plugins.scala.lang.psi.api.base.ScInterpolatedStringLiteral
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createExpressionFromText

@ExtensionImpl
class LegacyStringFormattingInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.legacy.string.formatting"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def isEnabledByDefault: Boolean = false

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case element@ConcatenationOrFormattingTopmostStringParts(parts) if parts.sizeIs > 1 =>
      val fix = (new FormattingQuickFix(element))
      holder.registerProblem(element, ScalaInspectionBundle.message("legacy.string.formatting.use.interpolated.string"), fix)
    case _ =>
  }
}

object LegacyStringFormattingInspection {
  private object ConcatenationOrFormattingTopmostStringParts extends TopmostStringParts(ConcatenationOrFormattingStringParser)

  private object ConcatenationOrFormattingStringParser extends StringParser {
    override def parse(element: PsiElement): Option[Seq[StringPart]] =
      StringConcatenationParser.parse(element).orElse(FormattedStringParser.parse(element))
  }

  private class FormattingQuickFix(element: PsiElement) extends PsiUpdateFix[PsiElement](element) {
    override def getFamilyName: String = ScalaInspectionBundle.message("convert.to.interpolated.string")

    override protected def invoke(context: PsiUpdateContext, element: PsiElement): Unit =
      AnyStringParser.parse(element).foreach { parts =>
        val expression = createExpressionFromText(InterpolatedStringFormatter(ScInterpolatedStringLiteral.Kind.Standard).format(parts), element)(using context.project)
        element.replace(expression)
      }
  }
}
