package org.jetbrains.plugins.scala.codeInspection.modifiers

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.application.dumb.DumbAware
import consulo.language.psi.{PsiElementVisitor, PsiFile}
import org.jetbrains.plugins.scala.codeInspection.PsiElementVisitorSimple
import org.jetbrains.plugins.scala.lang.lexer.{ScalaModifier, ScalaTokenTypes}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.plugins.scala.api.project.ScalaLanguageLevel

@ExtensionImpl
final class RedundantFinalOnToplevelObjectInspection extends LocalInspectionTool with DumbAware {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("final.modifier.is.redundant.for.toplevel.objects"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaInspectionBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE

  override def isAvailableForFile(file: PsiFile): Boolean = file match {
    case file: ScalaFile =>
      // Since Scala 2.13 'final' is redundant on all objects.
      // We add that warning in [[org.jetbrains.plugins.scala.annotator.modifiers.ModifierChecker]]
      file.scalaLanguageLevel.forall(_ < ScalaLanguageLevel.Scala_2_13)
    case _ =>
      false
  }

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case obj@ScObject.withModifierList(ml) if obj.isTopLevel && ml.isFinal =>
      val finalModifier = ml.findFirstChildByType(ScalaTokenTypes.kFINAL).getOrElse(obj.targetToken)
      val quickFix = new SetModifierQuickfix(obj, ScalaModifier.Final, set = false)
      holder.registerProblem(finalModifier, getDisplayName.get, quickFix)
    case _ =>
  }
}
