package org.jetbrains.plugins.scala.codeInspection.deprecation

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemHighlightType, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement

@ExtensionImpl(id = "ScalaDeprecatedIdentifier")
final class ScalaDeprecatedIdentifierInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.deprecated.identifier"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  import ScalaDeprecatedIdentifierInspection._

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case ref: ScReference if deprecatedNames.contains(ref.refName) =>
      holder.registerProblem(
        ref.nameId,
        ScalaInspectionBundle.message("usage.of.deprecatedname.as.identifier.is.deprecated", ref.refName),
        ProblemHighlightType.LIKE_DEPRECATED,
      )
    case named: ScNamedElement if deprecatedNames.contains(named.name) =>
      holder.registerProblem(
        named.nameId,
        ScalaInspectionBundle.message("usage.of.deprecatedname.as.identifier.is.deprecated", named.name),
        ProblemHighlightType.LIKE_DEPRECATED,
      )
    case _ =>
  }
}

object ScalaDeprecatedIdentifierInspection {
  private[ScalaDeprecatedIdentifierInspection] val deprecatedNames = Set("then")
}
