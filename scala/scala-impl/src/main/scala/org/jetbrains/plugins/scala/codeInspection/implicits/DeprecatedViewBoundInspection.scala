package org.jetbrains.plugins.scala.codeInspection.implicits

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.project.Project
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInsight.intention.types.ConvertImplicitBoundsToImplicitParameter._
import org.jetbrains.plugins.scala.codeInspection.implicits.DeprecatedViewBoundInspection._
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypeBoundsOwner

@ExtensionImpl
final class DeprecatedViewBoundInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.view.bounds.are.deprecated"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case boundsOwner: ScTypeBoundsOwner if boundsOwner.viewBound.nonEmpty && canBeConverted(boundsOwner) =>
      holder.registerProblem(boundsOwner, description, new ConvertToImplicitParametersQuickFix(boundsOwner))
    case _ =>
  }
}

final class ConvertToImplicitParametersQuickFix(owner: ScTypeBoundsOwner)
  extends AbstractFixOnPsiElement(fixDescription, owner)
    with DumbAware {
  override protected def doApplyFix(boundOwner: ScTypeBoundsOwner)
                                   (implicit project: Project): Unit = {
    val addedParams = doConversion(boundOwner)
    runRenamingTemplate(addedParams)
  }
}

object DeprecatedViewBoundInspection {
  @Nls
  val description: String = ScalaInspectionBundle.message("displayname.view.bounds.are.deprecated")
  @Nls
  val fixDescription: String = ScalaInspectionBundle.message("replace.with.implicit.parameters")
}
