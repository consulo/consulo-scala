package org.jetbrains.plugins.scala.codeInspection.typeChecking

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.codeInspection.{PsiElementVisitorSimple, ScalaInspectionBundle}

@ExtensionImpl
class IsInstanceOfInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("displayname.isinstanceof"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case element@IsInstanceOfCall.withoutExplicitType() =>
      holder.registerProblem(element, ScalaInspectionBundle.message("missing.explicit.type.in.isinstanceof.call"))
    case _ =>
  }
}
