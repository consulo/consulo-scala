package org.jetbrains.plugins.scala.codeInspection.caseClassParamInspection

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInspection.PsiElementVisitorSimple
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScModifierListOwner
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.util.EnumSet._

@ExtensionImpl(id = "CaseClassParam")
final class CaseClassParamInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.case.class.parameter"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case c: ScClass if c.isCase =>
      for {
        paramClause <- c.allClauses.take(1)
        case classParam@(__ : ScClassParameter) <- paramClause.parameters
        if classParam.isVal && classParam.isCaseClassPrimaryParameter && !hasExplicitModifier(classParam)
      } {
        val valToken = classParam.findFirstChildByType(ScalaTokenTypes.kVAL)
        val errorElement = valToken.getOrElse(classParam)

        holder.registerProblem(
          errorElement,
          ScalaBundle.message("val.on.case.class.param.redundant"),
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
          new RemoveValQuickFix(classParam)
        )
      }
    case _ =>
  }

  private def hasExplicitModifier(owner: ScModifierListOwner): Boolean = !owner.getModifierList.modifiers.isEmpty
}
