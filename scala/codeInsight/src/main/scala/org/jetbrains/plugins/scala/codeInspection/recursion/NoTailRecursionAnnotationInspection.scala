package org.jetbrains.plugins.scala.codeInspection.recursion

import consulo.annotation.component.ExtensionImpl
import consulo.language.Language
import consulo.language.editor.rawHighlight.HighlightDisplayLevel
import consulo.localize.LocalizeValue
import org.jetbrains.plugins.scala.api.ScalaLanguage

import consulo.language.editor.inspection.{LocalInspectionTool, ProblemsHolder}
import consulo.project.Project
import consulo.language.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.annotator.FunctionAnnotator
import org.jetbrains.plugins.scala.codeInsight.intention
import org.jetbrains.plugins.scala.codeInsight.messages.ScalaCodeInsightBundle
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, PsiElementVisitorSimple}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.util.IntentionAvailabilityChecker

@ExtensionImpl
final class NoTailRecursionAnnotationInspection extends LocalInspectionTool {
  override def getDisplayName: LocalizeValue = LocalizeValue.of(ScalaCodeInsightBundle.message("no.tailrec.annotation.title"))

  override def getGroupDisplayName: LocalizeValue = LocalizeValue.of(ScalaCodeInsightBundle.message("group.general"))

  override def getDefaultLevel: HighlightDisplayLevel = HighlightDisplayLevel.WARNING

  override def getLanguage: Language = ScalaLanguage.INSTANCE


  import intention.recursion.AddTailRecursionAnnotationIntention._

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = PsiElementVisitorSimple(holder) {
    case element@CanBeTailRecursive(function)
      if IntentionAvailabilityChecker.checkInspection(this, element) &&
         FunctionAnnotator.canBeTailRecursive(function) =>
      val quickFix = new AbstractFixOnPsiElement(
        ScalaCodeInsightBundle.message("no.tailrec.annotation.fix"),
        function
      ) {
        override protected def doApplyFix(function: ScFunctionDefinition)
                                         (implicit project: Project): Unit = {
          addTailRecursionAnnotation(function)
        }
      }

      holder.registerProblem(element, getDisplayName, quickFix)
    case _ =>
  }
}
